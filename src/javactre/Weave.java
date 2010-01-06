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
public class Weave {

    String          page_name;

    String          weave5c;
    String          weave3;
    String          deps5c;
    String          text3;
    String          text1;
    String          feed_awareness[];
    StringBuffer    patch5c;
    String          version2;
    String          patch_version2;
    ArrayList<String> sources = new ArrayList<String>();

    static diff_match_patch fraser = new diff_match_patch();

    public Weave (String name) {
        page_name = name;
        weave5c = "ॐ\0\0\0\0۝\0\0\0\1";
        version2 = "\0\1";
        patch_version2 = version2;
        sources.add("ॐ default feed ۝");
    }

    public void loadSourcesFromString (String sources) {

    }

    public void loadWeave5cFromString (String weave5c) {
        if (!weave5c.startsWith("ॐ\0\0\0\0"))
            throw new IllegalArgumentException("weave misses root atom");
        if (!weave5c.endsWith("۝\0\0\0\1"))
            throw new IllegalArgumentException("weave misses terminator");
        if (weave5c.length()%5!=0)
            throw new IllegalArgumentException("incorrect weave5c length");
        this.weave5c = weave5c;
        weave3 = deps5c = text3 = text1 =
                version2 = patch_version2 = null;
        patch5c = new StringBuffer();
        // feed_awareness
    }

    String  getName () {
        return page_name;
    }

    String  getWeave5c () {
        return weave5c;
    }

    String  getWeave3 () {
        if (weave3==null)
            weave3 = weave5c.replaceAll("(.)..(..)","$1$2");
        return weave3;
    }

    String  getText1 () {
        if (text1==null)
            text1 = getText3().replaceAll("(.)..","$1");
        return text1;
    }
    
    String  getText3 () {
        if (text3==null)
            text3 = weave5c2text3(weave5c);
        return text3;
    }

    String  getText3Version (String version) {
        String filtre = version2filtre(version);
        String filtered = weave5c.replaceAll("(...("+filtre+"))|.{5}","$1");
        String text3fd = weave5c2text3(filtered);
        return text3fd;
    }

    String  getText1Version (String version) {
        return getText3Version(version).replaceAll("(.)..","$1");
    }

    String  getVersion1 () {
        return this.version2.replaceAll(".(.)", "$1").substring(1);
    }

    String  getVersion2 () {
        return version2.substring(1);
    }

    String  getSource (char code) {
        return sources.get((int)code);
    }

    char    getSourceCode (String url) {
        int i = sources.indexOf(url);
        if (i==-1) {
            char code = (char)sources.size();
            sources.add(url);
            patch_version2 += code;
            patch_version2 += '\0';
            version2 += code;
            version2 += '\0';
            return code;
        } else
            return Character.toChars(i)[0];
    }

    String  addPatch3c (String patch3c, String source) {
        char src = getSourceCode(source);
        int srci = (int)src;
        int cur = patch_version2.codePointAt(((int)src)*2+1);
        if (patch5c==null)
            patch5c = new StringBuffer();
        for(int i=0; i<patch3c.length()/3; i++) {
            patch5c.append(patch3c.substring(i*3, i*3+3));
            patch5c.append(src);
            patch5c.append((char)(cur++));
        }
        patch_version2 = patch_version2.substring(0, srci*2+1)
                + (char)cur + patch_version2.substring(srci*2+2);
        return patch_version2;
    }

    String  insert (int pos, String str, String source) {
        char src = getSourceCode(source);
        int feedlen = this.patch_version2.charAt(1+2*(int)src);
        String cause = pos==0 ? "\u0950\0\0" : getText3().substring(pos*3-3,pos*3);
        StringBuffer patch = new StringBuffer();
        patch.append(str.charAt(0));
        patch.append(cause.charAt(1));
        patch.append(cause.charAt(2));
        for(int i=1; i<str.length(); i++) {
            patch.append(str.charAt(i));
            patch.append(src);
            patch.append((char)(feedlen+i-1));
        }
        return addPatch3c(patch.toString(),source);
    }

    /** Deletes text at the position in the current version
        of the text. */
    String  delete (int pos, int length, String source) {
        String chunk = getText3().substring(pos*3, (pos+length)*3);
        String patch = chunk.replaceAll(".(..)","\b$1");
        return addPatch3c(patch,source);
    }


    String  getDependencies5c () {
        if (this.deps5c==null)
            deps5c = weave5c.replaceAll(".(.).\\1.|(.{5})", "$2");
        return deps5c;
    }


    /** Gives a crude version of     */
    String  getAwarenessClosure (String ver2) {
        String closure;
        while ( !ver2.equals(closure=getAwareness(ver2)) )
            ver2 = closure;
        return closure;
    }

    /** For a given atom (defined by <i>fo</i> position string) gives its
        entire awareness cone, i.e. a version2 string covering atoms it is
        aware of. */
    String  getAwareness (String ver2) {
        String dep5c = getDependencies5c();
        String filtre = version2filtre(ver2); // FIXME: patch5c
        // take dependencies of atoms under ver2
        String filt2 = dep5c.replaceAll(".(..)("+filtre+")|.{5}", "$1");
        // remove dependencies under ver2 already
        filt2 = filt2.replaceAll("("+filtre+")|(..)", "$2");
        if (filt2.length()==0)
            return ver2;
        return redundant2version(filt2+ver2);
    }


    char    feedLength (char feed) {
        return 'z';
    }
  

    protected void  insertDeletionSegment (String bs5c) {
        // compose regex
        String headver = bs5c.substring(3,5);
        String victims = positions2filtre(bs5c.replaceAll(".(..)..", "$1"));
        // insert deletions, just after
        weave5c = weave5c.replaceAll
                ("(.{5}*?)(...("+victims+"))","$1$2\b$3"+headver);
        // you may undelete only deletes you are aware of!!!
        // mass deletions may claim they have the same offset in a feed
        // we dont care that much about offsets of backspaces as nothing
        //      is inserted after a backspace
        // FIXED possibility of an explosion
    }


    protected void  insertUndeletionSegment (String undo5c) {
        String headver = undo5c.substring(3,5);
        String affects = version2filtre(getAwarenessClosure(headver));
        String recovers = positions2filtre(undo5c.replaceAll(".(..)..", "$1"));
        weave5c = weave5c.replaceAll(
                "(.{5}*?)(\b("+recovers+")("+affects+"))",
                "$1\7$3"+headver+"$2"
                );
    }


    protected void  insertMarkSegment (String marks5c) {
        weave5c += marks5c; // append after ۝
    }


    protected void  insertSegment (String segment5c) {
        String attach_pos = segment5c.substring(1, 3);
        if (feedLength(attach_pos.charAt(0))<attach_pos.charAt(1))
            throw new IllegalArgumentException("deps lacking");
        char feed = segment5c.charAt(3);
        char start = segment5c.charAt(4);
        if (feedLength(feed)!=start)
            throw new IllegalArgumentException("feed gap");
        String aware_cone = flatten_version(feed_awareness[feed]+attach_pos);
        String aware_filtre = version2filtre(aware_cone);
        String new_weave5c = this.weave5c.replaceFirst(
                "^(.{5}*?)(..."+attach_pos+"([\b\7\0]"+attach_pos+
                    "..)*)(?=...("+aware_filtre+"))",
                "$1$2"+segment5c
                );
        if (new_weave5c.length()==weave5c.length()) { // tough segment
            aware_cone = getAwarenessClosure(aware_cone);
            // grab siblings
            String siblings = this.weave5c.replaceAll
                    ("([^\b\7\0]"+attach_pos+"..)|.{5}", "$1");
            // loop; find limiting
            String insert_before = "(?!."+attach_pos+"..)(.("+aware_filtre+")..)";
            int i=0;
            while (i<siblings.length()) {
                String sibling = siblings.substring(i,i+2);
                if (aware_cone.compareTo(getAwarenessClosure(sibling)) > 0) {
                    insert_before = "."+attach_pos+sibling;
                    break;
                }
                i+=2;
            }
            // insert by re
            new_weave5c = this.weave5c.replaceFirst(
                "^(.{5}*?)(..."+attach_pos+")(.{5})*?("+insert_before+")",
                "$1$2$3"+segment5c+"$4"
                );
        }
        this.weave5c = new_weave5c;
    }

    
    /** Integrates patches into the weave; returns the
        resuting version string. */
    public void     addPatch5c (String patch5c) {
        // split into insert/delete/undelete/aware segments
        Pattern segments = Pattern.compile
                ("((?:[\b\7\0].{4})*)|(...(?:(..).\\3)*..)");
        Matcher m = segments.matcher(patch5c);
        while (m.find()) {
            if (m.group(1)!=null) {
                switch (m.group(1).charAt(0)) {
                    case '\b':  insertDeletionSegment(m.group(1));
                    case '\7':  insertUndeletionSegment(m.group(1));
                    case '\0':  insertMarkSegment(m.group(1));
                }
            } else
                insertSegment(m.group(2));
        }
        this.weave3 = this.deps5c = this.text1 = this.text3 = null;
        //this.feed_awareness[feed] = aware_cone;
        //this.version2 = flatten_version(this.version2 + feed + start);
        // FIXME
    }


    String  setNewTextVersion (String new_text, String source) {
        String text = getText1();
        LinkedList<diff_match_patch.Diff> diffs =
                fraser.diff_main(text, new_text, true);
        fraser.diff_cleanupSemantic(diffs);
        //new ArrayList<diff_match_patch.Diff>
        for(int i=0; i<diffs.size()-1; i++)
            if (diffs.get(i).operation==diff_match_patch.Operation.DELETE &&
                diffs.get(i+1).operation==diff_match_patch.Operation.INSERT)
                Collections.swap(diffs, i, i+1);
        int pos = 0;
        for(diff_match_patch.Diff diff : diffs) {
            if (diff.operation == diff_match_patch.Operation.DELETE) {
                delete(pos,diff.text.length(),source);
                pos += diff.text.length();
            } else if (diff.operation == diff_match_patch.Operation.INSERT) {
                insert(pos,diff.text,source);
            } else { // EQUAL
                pos += diff.text.length();
            }
        }
        //compile();
        return version2;
    }


    static String re_screen (String str) {
        String screened = str.replaceAll
                ("([\\\\\\^\\$\\.\\[\\]\\|\\(\\)\\?\\*\\+\\{\\}])", "\\\\$1");
        //System.err.println("screened: "+screened);
        return screened;
    }


    static public String flatten_version (String version) {
        return version;
    }


    /** Print string, escaping non-printable chars. */
    public static void printesc (String s) {
        for(int i=0; i<s.length(); i++)
            if (s.charAt(i)>=' ')
                System.out.append(s.charAt(i));
            else
                System.out.append("\\"+(int)s.charAt(i));
        System.out.println();
    }


    static String   version2filtre (String ver2) {
        ver2 = re_screen(ver2);
        String re = ver2.replaceAll("(\\\\.|.)(\\\\.|.)", "|$1[\u0000-$2]");
        return "\0[\0-\1]"+re;
    }


    static String   positions2filtre (String pos2) {
        pos2 = re_screen(pos2);
        String re = pos2.replaceAll("(\\\\.|.)(\\\\.|.)", "|$1$2");
        return re.substring(1);
    }


    /** Removes redundant pairs from a version2 string. */
    public static String redundant2version (String redundant) {
        String screened = re_screen(redundant);
        String rex = screened.replaceAll("(\\\\.|.)(\\\\.|.)", "$1[^$2-\uffff]|");
        //System.err.println("rex: "+rex);
        String brief = redundant.replaceAll("("+rex+"\0.)|(..)", "$2");
        return brief;
    }


    public static String weave5c2text3 (String weave5c) {
        String text3c = weave5c.replaceAll(
                "(...(..))(\b\\2..)+|[\b\u0000].{4}|.\0.\0.|(.)..(..)",
                "$4$5"
                ); // FIXME: each deletion must be undone separately
        return text3c;
    }


}
