package com.sk89q.craftbook.sponge.mechanics.ics.plc;

import com.sk89q.craftbook.sponge.CraftBookPlugin;
import com.sk89q.craftbook.sponge.mechanics.ics.IC;
import com.sk89q.craftbook.sponge.mechanics.ics.InvalidICException;
import com.sk89q.craftbook.sponge.mechanics.ics.plc.lang.WithLineInfo;
import com.sk89q.craftbook.sponge.util.SignUtil;
import com.sk89q.worldedit.BlockWorldVector;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class PlcIC<Lang extends PlcLanguage> extends IC {
    private Lang lang;
    public PlcFactory.PlcStateData state;
    private String codeString;
    private WithLineInfo[] code;

    private boolean error = false;
    private String errorString = "no error";

    PlcIC(PlcFactory<Lang> factory, Location<World> location, Lang lang) {
        super(factory, location);
        this.lang = lang;
    }

    @Override
    public void create(Player player, List<Text> lines) throws InvalidICException {
        super.create(player, lines);

        try {
            codeString = getCode();
        } catch (CodeNotFoundException e) {
            throw new InvalidICException("Error retrieving code: " + e.getMessage());
        }
        try {
            if (codeString != null) {
                code = lang.compile(codeString);
            }
        } catch (InvalidICException e) {
            throw new RuntimeException("inconsistent compile check!", e);
        }
        state.state = lang.initState();
    }

    private boolean isShared() {
        return !getLine(3).isEmpty() && getLine(3).startsWith("id:");
    }

    private String getID() {
        return getLine(2);
    }

    private String getFileName() {
        if (!isShared()) {
            BlockWorldVector l = sign.getBlockVector();
            return lang.getName() + "$$" + l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ();
        } else return lang.getName() + "$" + getLine(3);
    }

    private File getStorageLocation() {
        World w = BukkitUtil.toWorld(sign.getLocalWorld());
        File worldDir = w.getWorldFolder();
        File targetDir = new File(new File(worldDir, "craftbook"), "plcs");
        if(new File(worldDir, "craftbook-plcs").exists()) {

            File oldFolder = new File(worldDir, "craftbook-plcs");
            if(!targetDir.exists())
                targetDir.mkdirs();
            if(!oldFolder.renameTo(targetDir))
                logger.warning("Failed to copy PLC States over to new directory!");
            oldFolder.delete();
        }
        targetDir.mkdirs();
        return new File(targetDir, getFileName());
    }

    private String hashCode(String code) {
        if(code == null)
            return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(code.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte aDigest : digest) {
                String byteHex = Integer.toHexString(aDigest & 0xFF);
                if (byteHex.length() == 1) {
                    hex.append('0');
                }
                hex.append(byteHex);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("insane JVM implementation", e);
        }
    }

    private void saveState() throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(getStorageLocation()))) {
            out.writeInt(PLC_STORE_VERSION);
            out.writeBoolean(error);
            out.writeUTF(errorString);
            out.writeUTF(lang.getName());
            out.writeUTF(error ? "(error)" : getID());
            out.writeUTF(hashCode(codeString));
            lang.writeState(state, out);
        }
    }

    private String getBookCode(Location<World> chestBlock) throws CodeNotFoundException {
        Chest c = chestBlock.getTileEntity().map(te -> (Chest) te).get();
        Inventory i = c.getDoubleChestInventory().orElse(c.getInventory());
        ItemStack book = null;
        for (Inventory inventory : i.query(ItemTypes.WRITABLE_BOOK, ItemTypes.WRITTEN_BOOK)) {
            if (s != null && s.getAmount() > 0 && (s.getType() == Material.BOOK_AND_QUILL || s.getType() == Material.WRITTEN_BOOK)) {
                if (book != null) throw new CodeNotFoundException("More than one written book found in chest!!");
                book = s;
            }
        }
        if (book == null) throw new CodeNotFoundException("No written books found in chest.");

        StringBuilder code = new StringBuilder();
        for (String s : ((BookMeta) book.getItemMeta()).getPages()) {
            code.append(s).append("\n");
        }
        CraftBookPlugin.logDebugMessage(code.toString(), "plc");
        return code.toString();
    }

    private String getCode() throws CodeNotFoundException {
        Location<World> above = getBlock().getRelative(Direction.UP);
        if (above.getBlockType() == BlockTypes.CHEST) return getBookCode(above);
        Location<World> below = getBlock().getRelative(Direction.DOWN);
        if (below.getBlockType() == BlockTypes.CHEST) return getBookCode(below);

        int x = getBlock().getBlockX();
        int z = getBlock().getBlockZ();

        for (int y = 0; y < 256; y++) {
            if (y != getBlock().getBlockY()) {
                Location<World> testBlock = getBlock().getExtent().getLocation(x, y, z);
                if (SignUtil.isSign(testBlock)) {
                    Sign sign = testBlock.getTileEntity().map(te -> (Sign) te).get();
                    if (SignUtil.getTextRaw(sign, 1).equalsIgnoreCase("[Code Block]")) {
                        y--;
                        Location<World> b = testBlock.getExtent().getLocation(x, y, z);
                        StringBuilder code = new StringBuilder();
                        while (SignUtil.isSign(b)) {
                            sign = b.getTileEntity().map(te -> (Sign) te).get();
                            for (int li = 0; li < 4 && y != getBlock().getBlockY(); li++) {
                                code.append(SignUtil.getTextRaw(sign, li)).append('\n');
                            }
                            b = getBlock().getExtent().getLocation(x, --y, z);
                        }
                        return code.toString();
                    }
                }
            }
        }
        throw new CodeNotFoundException("No code source found.");
    }

    public void error(String shortMessage, String detailedMessage) {
        setLine(2, Text.of(TextColors.RED, "!Error!"));
        setLine(3, Text.of(shortMessage));

        error = true;
        errorString = detailedMessage;

        trySaveState();
    }

    @Override
    public void trigger() {
        try {
            if (isShared()) {
                tryLoadState();
            }

            lang.execute(this, state.state, code);

            trySaveState();
        } catch (PlcException e) {
            error(e.getMessage(), e.detailedMessage);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Internal error while executing PLC", e);
            error(e.getClass().getName(), "Internal error encountered: " + e.getClass().getName());
        }
    }

    /*@Override
    public void onRightClick(Player p) {
        if (CraftBookPlugin.inst().hasPermission(p, "craftbook.plc.debug")) {
            p.sendMessage(ChatColor.GREEN + "Programmable Logic Controller debug information");
            BlockWorldVector l = sign.getBlockVector();
            p.sendMessage(ChatColor.RED + "Status:" + ChatColor.RESET + " " + (error ? "Error Encountered" : "OK"));
            p.sendMessage(ChatColor.RED + "Location:" + ChatColor.RESET + " (" + l.getBlockX() + ", " +
                    "" + l.getBlockY() + ", " + l.getBlockZ() + ")");
            p.sendMessage(ChatColor.RED + "Language:" + ChatColor.RESET + " " + lang.getName());
            p.sendMessage(ChatColor.RED + "Full Storage Name:" + ChatColor.RESET + " " + getFileName());
            if (error) {
                p.sendMessage(errorString);
            } else {
                p.sendMessage(lang.dumpState(state.state));
            }
        } else {
            p.sendMessage(ChatColor.RED + "You do not have the necessary permissions to do that.");
        }
    }*/
}