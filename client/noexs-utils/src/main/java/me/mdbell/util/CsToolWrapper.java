package me.mdbell.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CsToolWrapper {

    static Pattern pattern = Pattern.compile("^\\W*([0-9a-fA-F]*)\\W{2}([0-9a-fA-F\\W]*)\\W{2}(.*)$");

    public static byte[] assemble(String str, long addr) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        String[] args = {"kstool", "arm64", str, HexUtils.formatAddress(addr)};
        Process p = pb.command(args).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = br.readLine();
        if(line == null || line.startsWith("ERROR")) {
            p.destroy();
            return null;
        }
        int idx1 = line.lastIndexOf('[');
        int idx2 = line.lastIndexOf(']');
        String split = line.substring(idx1 + 1, idx2);
        String[] data = split.split("\\s");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for(int i = 0; i < data.length; i++) {
            String s = data[i].trim();
            if(s.length() > 0) {
                out.write(Integer.parseInt(s, 16));
            }
        }
        out.flush();
        byte[] res = out.toByteArray();
        p.destroy();
        return res;
    }

    public static List<Insn> disasm(byte[] code, long addr) throws IOException {
        ProcessBuilder pc = new ProcessBuilder();
        String bytes = encodeBytes(code);
        String[] args = {"cstool", "arm64", bytes, HexUtils.formatAddress(addr)};
        Process p = pc.command(args).start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        ArrayList<Insn> insns = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            Matcher m = pattern.matcher(line);
            if(m.matches()) {
                long address = Long.parseUnsignedLong(m.group(1), 16);
                String mem = m.group(2).trim();
                String op = m.group(3).replace('\t', ' ').trim();
                insns.add(new Insn(address, mem, op));
            }else{
                //we had an error parsing
            }
        }
        p.destroy();
        return insns;
    }

    private static String encodeBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int i = b & 0xFF;
            if (i < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toUnsignedString(i, 16).toUpperCase());
        }
        return sb.toString().trim();
    }

    public static class Insn {

        private Insn(long addr, String mnemonic, String opstr) {
            this.addr = addr;
            this.mnemonic = mnemonic;
            this.opstr = opstr;
        }
        long addr;
        String mnemonic;
        String opstr;

        public long getAddr(){
            return addr;
        }

        public String getMnemonic(){
            return mnemonic;
        }

        public String getOpStr(){
            return opstr;
        }

        @Override
        public String toString() {
            return String.format("%s: %s %s", HexUtils.formatAddress(addr),
                    HexUtils.pad('0', 8, mnemonic).toUpperCase(), opstr);
            //return HexUtils.formatAddress(addr) + "\t\t" + HexUtils.pad('0', 8, mnemonic).toUpperCase() + "\t\t" + opstr;
        }
    }

}
