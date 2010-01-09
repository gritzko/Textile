/*
 *  Java CT-RE
 *  Version Control in Causal Trees
 *  Implementated in Regular Expressions
 *  (c) Victor Grishchenko 2010
 */
package javactre;

import name.fraser.neil.plaintext.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author victor
 */
public class Textile {

    String page_name;
    String weave5c;
    String weave3;
    String deps5c;
    String text3;
    String text1;
    ArrayList<String> yarn_aware;
    String weft2;
    ArrayList<String> yarn_sources;
    static diff_match_patch fraser = new diff_match_patch();
    static public char YARN_CODE_START = 'a';
    static public char YARN_LENGTH_START = '1';

    public Textile(String name) {
        page_name = name;
        weave5c = "ॐ\0\0\0\0۝\0\0\0\1";
        weft2 = "\0\1";
        yarn_sources = new ArrayList<String>(64);
        yarn_aware = new ArrayList<String>(64);
        yarn_sources.add("ॐ divine yarn ۝");
        yarn_aware.add(weft2);
        while (yarn_sources.size()<YARN_CODE_START)
            yarn_sources.add(null);
        while (yarn_aware.size()<YARN_CODE_START)
            yarn_aware.add(null);
    }


    public void loadSourcesFromString(String source_list) {
        if (yarn_sources.size()!=YARN_CODE_START)
            throw new IllegalStateException("class is not clean already");
        String uris [] = source_list.split("\n");
        for(int i=0; i<uris.length; i++)
            if (uris[i].length()>0)
                yarn_sources.add(uris[i]);
    }


    public void loadWeave5cFromString(String weave5c) {
        if (!weave5c.matches("ॐ\0\0\0\0(.{5})*?۝\0\0\0\1(\0.{4})*"))
            throw new IllegalArgumentException("data format error");
        this.weave5c = weave5c;
        weave3 = deps5c = text3 = text1 = null;
        weft2 = find_weft2(weave5c);
        String deps = getDependencies5c();
        StringBuilder wefts [] = new StringBuilder[1<<16];
        char max_yarn = '\0';
        for(int i=0; i<deps.length()/5; i++) {
            char yarn = deps.charAt(i*5+3);
            if (yarn>max_yarn)
                max_yarn = yarn;
            String dep = deps.substring(i*5+1, i*5+3);
            if (wefts[yarn]==null)
                wefts[yarn] = new StringBuilder();
            wefts[yarn].append(dep);
        }
        yarn_aware.clear();
        for(int i=0; i<=max_yarn; i++)
            if (wefts[i]!=null)
                yarn_aware.add(straighten_weft2(wefts[i].toString()));
            else
                yarn_aware.add("\0\2");
    }


    String getName() {
        return page_name;
    }

    String getWeave5c() {
        return weave5c;
    }

    String getWeave3() {
        if (weave3 == null) {
            weave3 = weave5c.replaceAll("(.)..(..)", "$1$2");
        }
        return weave3;
    }

    String getText1() {
        if (text1 == null) {
            text1 = getText3().replaceAll("(.)..", "$1");
        }
        return text1;
    }

    String getText3() {
        if (text3 == null) {
            text3 = weave5c2text3(weave5c);
        }
        return text3;
    }

    String getText3ByWeft2(String version) {
        String filtre = weft2re(version);
        String filtered = weave5c.replaceAll("(...(" + filtre + "))|.{5}", "$1");
        String text3fd = weave5c2text3(filtered);
        return text3fd;
    }

    String getText1ByWeft2(String version) {
        return getText3ByWeft2(version).replaceAll("(.)..", "$1");
    }

    public String getWeft1() {
        return this.weft2.replaceAll(".(.)", "$1").substring(1);
    }

    public String getWeft2() {
        return weft2.substring(1);
    }


    public char addNewSource (String url) {
        if (yarn_sources.size()!=yarn_aware.size())
            throw new IllegalStateException("yarn_sources/yarn_aware mismatch");
        yarn_sources.add(url);
        yarn_aware.add("\0\2");
        return (char)(yarn_sources.size()-1);
    }


    public String getSourceUri(char code) {
        return yarn_sources.get((int) code);
    }

    public char getSourceCode(String uri) {
        int i = yarn_sources.indexOf(uri);
        return i==-1 ? '\0' : (char)i;
    }


    /** Implied: atoms are sequential. */
    public String addPatch3c(String patch3c, String source) {
        char yarn = this.getSourceCode(source);
        if (yarn=='\0')
            throw new IllegalArgumentException("unknown source URI");
        int len = (int)this.getYarnLength(yarn);
        StringBuilder patch5c = new StringBuilder();
        for (int i = 0; i < patch3c.length() / 3; i++) {
            patch5c.append(patch3c.charAt(i*3));
            String cause = patch3c.substring(i*3+1, i*3+3);
            if (cause.equals("\0\2")) {
                patch5c.append(yarn);
                patch5c.append((char)len);
            } else
                patch5c.append(cause);
            patch5c.append(yarn);
            patch5c.append((char)++len);
        }
        return addPatch5c(patch5c.toString());
    }


    public String getInsertionPatch3c(int pos, String str) {
        String cause = pos == 0 ? "\0\0" : getText3().substring(pos*3 - 2, pos*3);
        String right_sibling = pos==0 ? "" : weave5c.replaceAll
                ("."+re_screen(cause)+"(..).*|.{5}", "$1");
        // FIXME check everything for re_screen
        str = str.replaceAll("(.)", "$1\0\2");
        str = str.replaceFirst("^(.)\0\2", "$1"+cause);
        if (right_sibling.length()>0)
            str = '\0'+right_sibling+str;
        return str;
    }


    /** Deletes text at the position in the current version
    of the text. */
    public String getDeletionPatch3c (int pos, String erased) {
        String chunk = getText3().substring(pos * 3, (pos + erased.length()) * 3);
        String patch3c = chunk.replaceAll(".(..)", "\b$1");
        return patch3c;
    }


    public String   getDependencies5c() {
        if (this.deps5c == null) {
            deps5c = weave5c.replaceAll(".(.).\\1.|(.{5})", "$2");
        }
        return deps5c;
    }


    public String   getYarnAwareness (char yarn) {
        if (yarn_aware.size()-1<yarn)
            return "";
        return yarn_aware.get(yarn);
    }


    /** Gives a crude version of     */
    public String   getAwarenessClosure(String ver2) {
        String closure;
        while (!ver2.equals(closure = getAwareness(ver2))) {
            ver2 = closure;
        }
        return closure;
    }


    /** For a given atom (defined by <i>fo</i> position string) gives its
    entire awareness cone, i.e. a version2 string covering atoms it is
    aware of. */
    public String getAwareness(String ver2) {
        String dep5c = getDependencies5c();
        String filtre = weft2re(ver2); // FIXME: patch5c
        // take dependencies of atoms under ver2
        String filt2 = dep5c.replaceAll(".(..)(" + filtre + ")|.{5}", "$1");
        // remove dependencies under ver2 already
        filt2 = filt2.replaceAll("(" + filtre + ")|(..)", "$2");
        if (filt2.length() == 0) {
            return ver2;
        }
        return straighten_weft2(filt2 + ver2);
    }


    /*public String getSourceAwareness (char code) {
        return code<yarn_aware.size() ? yarn_aware.get(code) :
    }*/


    public String  sortWeft (String weft2) {
        String sep = weft2.replaceAll("\0.|.\0|(..)", "\0$1").
                replaceFirst("^\0+", "");
        String split [] = sep.split("\0+");
        Arrays.sort(split, new Comparator<String>(){
            public int compare(String o1, String o2) {
                char i1 = o1.charAt(0);
                char i2 = o2.charAt(0);
                return yarn_sources.get(i1).compareTo(yarn_sources.get(i2));
            }
        });
        StringBuilder b = new StringBuilder();
        for(int i=0; i<split.length; i++)
            b.append(split[i]);
        return b.toString();
    }


    public int compareWefts (String a, String b) {
        String fa = weft2re(a), fb = weft2re(b);
        a = a.replaceAll(fb+"|(.).", "$1");
        b = b.replaceAll(fa+"|(.).", "$1");
        if (a.length()==0)
            if (b.length()>0)
                return -1;
            else
                return 0;
        else if (b.length()==0)
            return 1;
        String min = "\uffff";
        for(int i=0; i<a.length(); i++) {
            String uri = getSourceUri(a.charAt(i));
            if (uri.compareTo(min)<0)
                min = uri;
        }
        for(int i=0; i<b.length(); i++) {
            String uri = getSourceUri(b.charAt(i));
            if (uri.compareTo(min)<0)
                return -1;
        }
        return 1;
    }

    
    protected void weaveInDeletionSegment(String bs5c) {
        // compose regex
        String headver = bs5c.substring(3, 5);
        String victims = positions2filtre(bs5c.replaceAll(".(..)..", "$1"));
        // insert deletions, just after
        weave5c = weave5c.replaceAll
                ( "((?:.{5})*?)(...(" + victims + "))((?:(?!...(?:"+victims+")).{5})+)",
                  "$1$2\b$3" + headver + "$4");
        // you may undelete only deletes you are aware of!!!
        // mass deletions may claim they have the same offset in a thread
        // we dont care that much about offsets of backspaces as nothing
        //      is inserted after a backspace
        // FIXED possibility of an explosion
    }


    protected void weaveInUndeletionSegment(String undo5c) {
        String headver = undo5c.substring(3, 5);
        String affects = weft2re(getAwarenessClosure(headver));
        String recovers = positions2filtre(undo5c.replaceAll(".(..)..", "$1"));
        weave5c = weave5c.replaceAll(
                "((?:.{5})*?)(\b(" + recovers + ")(" + affects + "))",
                "$1\7$3" + headver + "$2");
    }


    protected void weaveInMarkSegment(String marks5c) {
        weave5c += marks5c; // append after ۝
    }


    protected void weaveInTextSegment(String segment5c) {
        String attach_pos = segment5c.substring(1, 3);
        char yarn = segment5c.charAt(3);
        char start = segment5c.charAt(4);
        String aware_cone = flatten_version
                (getYarnAwareness(yarn) + attach_pos + segment5c.substring(3, 5));
        String aware_filtre = weft2re(aware_cone);
        String new_weave5c = this.weave5c.replaceFirst(
                "^((?:.{5})*?)(..." + attach_pos + "([\b\7\0]" + attach_pos
                + "..)*)(?=...(" + aware_filtre + "))",
                "$1$2" + segment5c);
        if (new_weave5c.length() == weave5c.length()) { // tough segment
            aware_cone = getAwarenessClosure(aware_cone);
            aware_cone = sortWeft(aware_cone);
            // grab siblings
            String siblings = this.weave5c.replaceAll
                    ("[^\b\7\0]" + attach_pos + "(..)|.{5}", "$1");
            // loop; find limiting
            String insert_before = "(?!." + attach_pos + "..)(.(" +
                    aware_filtre + ")..)";
            int i = 0;
            while (i < siblings.length()) {
                String sibling = siblings.substring(i, i + 2);
                String closure = getAwarenessClosure(sibling);
                String weft = sortWeft(closure);
                if (compareWefts(aware_cone,weft) > 0) {
                    insert_before = "." + attach_pos + sibling;
                    break;
                }
                i += 2;
            }
            // insert by re
            new_weave5c = this.weave5c.replaceFirst(
                    "^((?:.{5})*?)(..." + attach_pos +
                        ")((?:.{5})*?)(" + insert_before + ")",
                    "$1$2$3" + segment5c + "$4");
        }
        this.weave5c = new_weave5c;
    }
    

    /** Integrates patches into the weave; returns the
    resuting version string. */
    public String addPatch5c(String patch5c) {
        // sanity check
        String curweft = weft2re(this.weft2);
        String nopast = patch5c.replaceAll("...(?:"+curweft+")|(.{5})", "$1");
        if (nopast.length()!=patch5c.length())
            throw new IllegalArgumentException("patch contains past atoms");
        // also check nodeps, move to the loop
        // split into insert/delete/undelete/aware segments
        Pattern segments = Pattern.compile
                ("([\b\7\0])..(.).(?:\\1..\\2.)*|...(?:(..).\\3)*..");
        Matcher m = segments.matcher(patch5c);
        while (m.find()) {
            // here: check for out-of-order, test
            String segment = m.group();
            char symbol = segment.charAt(0);
            char source = segment.charAt(3);
            switch (symbol) {
                case '\b':
                    weaveInDeletionSegment(segment); break;
                case '\7':
                    weaveInUndeletionSegment(segment); break;
                case '\0':
                    weaveInMarkSegment(segment); break;
                default:
                    weaveInTextSegment(segment);
            }
            String deps = segment.replaceAll(".(....)", "$1");
            while (yarn_aware.size()-1<source)
                yarn_aware.add("\0\2");
            yarn_aware.set(source,straighten_weft2(yarn_aware.get(source)+deps));
        }
        this.weave3 = this.deps5c = this.text1 = this.text3 = null;
        this.weft2 = straighten_weft2(weft2+patch5c.replaceAll(".{3}(..)", "$1"));
        return weft2;
    }

    public String setNewText(String new_text, String source) {
        String text = getText1();
        LinkedList<diff_match_patch.Diff> lldiffs =
                fraser.diff_main(text, new_text, true);
        fraser.diff_cleanupSemantic(lldiffs);
        ArrayList<diff_match_patch.Diff> diffs = // FIXME
                new ArrayList<diff_match_patch.Diff>(lldiffs);
        for (int i = 0; i < diffs.size() - 1; i++) {
            if (diffs.get(i).operation == diff_match_patch.Operation.DELETE
                    && diffs.get(i + 1).operation == diff_match_patch.Operation.INSERT) {
                Collections.swap(diffs, i, i + 1);
            }
        }
        StringBuilder patch = new StringBuilder();
        int pos = 0;
        for (diff_match_patch.Diff diff : diffs) {
            if (diff.operation == diff_match_patch.Operation.DELETE) {
                patch.append(getDeletionPatch3c(pos, diff.text));
                pos += diff.text.length();
            } else if (diff.operation == diff_match_patch.Operation.INSERT) {
                patch.append(getInsertionPatch3c(pos, diff.text));
            } else { // EQUAL
                pos += diff.text.length();
            }
        }
        return addPatch3c(patch.toString(), source);
    }

    public char getYarnLength(char source_code) {
        if (yarn_aware.size()<source_code+1)
            return (char)((int)YARN_LENGTH_START-1);
        String self = yarn_aware.get(source_code).replaceAll
                ("("+source_code+".)|..", "$1");
        if (self.length()==0)
            return (char)((int)YARN_LENGTH_START-1); // FIXME PLEASE!!!
        return self.charAt(1);
    }

    public String markWeft(String source) {
        char code = getSourceCode(source);
        char mark = getYarnLength(code);
        mark++;
        String save_patch = weft2.replaceAll("(..)", "\0$1"+code+mark);
        addPatch5c(save_patch);
        return weft2;
    }


    /* . . . . . . . . . s t a t i c s . . . . . . . . . . */


    static String re_screen(String str) {
        String screened = str.replaceAll("([\\\\\\^\\$\\.\\[\\]\\|\\(\\)\\?\\*\\+\\{\\}])", "\\\\$1");
        //System.err.println("screened: "+screened);
        return screened;
    }

    static public String flatten_version(String version) {
        return version;
    }

    /** Print string, escaping non-printable chars. */
    static public void printesc(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= ' ') {
                System.out.append(s.charAt(i));
            } else {
                System.out.append("\\" + (int) s.charAt(i));
            }
        }
        System.out.println();
    }

    static String weft2re(String weft2) {
        weft2 = re_screen(weft2);
        String re = weft2.replaceAll("(\\\\.|.)(\\\\.|.)", "|$1[\u0000-$2]");
        return "\0[\0-\1]" + re;
    }

    static String positions2filtre(String pos2) {
        pos2 = re_screen(pos2);
        String re = pos2.replaceAll("(\\\\.|.)(\\\\.|.)", "|$1$2");
        return re.substring(1);
    }

    /** Removes redundant pairs from a version2 string. */
    static public String straighten_weft2(String redundant) {
        String screened = re_screen(redundant);
        String rex = screened.replaceAll("(\\\\.|.)(\\\\.|.)", "$1[^$2-\uffff]|");
        //System.err.println("rex: "+rex);
        String brief = redundant.replaceAll("(" + rex + "\0.)|(..)", "$2");
        return brief;
    }

    static public String weave5c2text3(String weave5c) {
        String text3c = weave5c.replaceAll(
                "(...(..))(\b\\2..)+|[\b\0\7].{4}|.\0.\0.|(.)..(..)",
                "$4$5"); // FIXME: each deletion must be undone separately
        return text3c;
    }


    static public String find_weft2 (String form5c) {
        String weft = "\0\1";
        while (form5c.length()>0) {
            String sub = 
                form5c.substring(0,Math.min(form5c.length(), 500));
            String newver = sub.replaceAll("...(..)","$1");
            weft = straighten_weft2(weft+newver);
            form5c = filter5c(form5c,weft);
        }
        return weft;
    }


    static public String filter5c (String form5c, String weft2) {
            String wre = weft2re(weft2);
            return form5c.replaceAll("...(?:"+wre+")|(.{5})", "$1");
    }

}
