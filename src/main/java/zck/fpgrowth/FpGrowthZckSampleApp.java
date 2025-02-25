/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package zck.fpgrowth;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public class FpGrowthZckSampleApp {
    private static class ItemSet{
        public ItemSet(Integer supCnt) {
            this.supCnt = supCnt;
        }
        public ArrayList<String> items = new ArrayList<String>();
        public Integer supCnt;
        public String getItemsRepr() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for(int i = 0; i < items.size(); i++) {
                sb.append(items.get(i));
                if(i != items.size()-1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            return sb.toString();
        }
    }
    private static class TreeNode {
        public TreeNode parent;
        public ArrayList<TreeNode> children = new ArrayList<>();
        public String item;
        public int count;
        public TreeNode next;
    }
    private static class CondPatternItemSet extends ItemSet {
        public CondPatternItemSet(Integer supCnt) {
            super(supCnt);
        }
        public int subTreeId;
    }
    private static boolean fpGrowth(String dataFilePath, int minSupCnt, double minConfThr) {
        BufferedReader br;
        ArrayList<ItemSet> C = new ArrayList<ItemSet>();
        ArrayList<ItemSet> freqItemSets = new ArrayList<ItemSet>();
        ArrayList<ArrayList<String>> dataCache = new ArrayList<>();
        ArrayList<String> itemsCache = new ArrayList<>();
        ArrayList<ItemSet> CSupDesc;
        try{
            br = new BufferedReader(new FileReader(dataFilePath));
            String line;
            br.readLine();
            HashMap<String, Integer> m = new HashMap<String, Integer>();
            while((line = br.readLine()) != null) {
                String[] tokens = line.split(" |, ");
                ArrayList<String> dataRow = new ArrayList<>();
                for(int i = 1; i < tokens.length; i++) {
                    if(m.get(tokens[i]) == null) {
                        m.put(tokens[i], 1);
                    } else {
                        m.put(tokens[i], m.get(tokens[i])+1);
                    }
                    dataRow.add(tokens[i]);
                }
                dataRow.sort(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.compareTo(o2);
                    }
                });
                dataCache.add(dataRow);
            }
            Iterator<String> it = m.keySet().iterator();
            while(it.hasNext()){
                String k = it.next();
                Integer v = m.get(k);
                itemsCache.add(k);
                if(v >= minSupCnt) {
                    ItemSet s = new ItemSet(v);
                    s.items.add(k);
                    C.add(s);
                }
            }
            // this makes sure that for any ItemSet, its items property is always ordered.
            C.sort(new Comparator<ItemSet>() {
                @Override
                public int compare(ItemSet o1, ItemSet o2) {
                    return o1.items.get(0).compareTo(o2.items.get(0));
                }
            });
            itemsCache.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });
            CSupDesc = (ArrayList<ItemSet>)C.clone();
            CSupDesc.sort(new Comparator<ItemSet>() {
                @Override
                public int compare(ItemSet o1, ItemSet o2) {
                    return o2.supCnt - o1.supCnt;
                }
            });
            br.close();
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch(IOException e){
            e.printStackTrace();
            return false;
        }

        // build the tree
        TreeNode root = new TreeNode();
        TreeNode[] lklist = new TreeNode[itemsCache.size()];
        for(int i = 0; i < lklist.length; i++) {
            lklist[i] = null;
        }
        for(ArrayList<String> row : dataCache) {
            ArrayList<ItemSet> t = new ArrayList<>();
            for(String itm : row) {
                int supCnt = 0;
                for(ItemSet s : CSupDesc) {
                    if(s.items.get(0).compareTo(itm) == 0) {
                        supCnt = s.supCnt;
                        break;
                    }
                }
                if(supCnt > 0) {
                    ItemSet s = new ItemSet(supCnt);
                    s.items.add(itm);
                    t.add(s);
                }
            }
            t.sort(new Comparator<ItemSet>() {
                @Override
                public int compare(ItemSet o1, ItemSet o2) {
                    return o2.supCnt - o1.supCnt;
                }
            });
            TreeNode node = root;
            for(ItemSet s : t) {
                if(node.children.stream().filter(o -> o.item == s.items.get(0)).findFirst().isPresent()) {
                    node.children.stream().filter(o -> o.item == s.items.get(0)).findFirst().get().count++;
                } else {
                    TreeNode newNode = new TreeNode();
                    newNode.parent = node;
                    newNode.item = s.items.get(0);
                    newNode.count = 1;
                    newNode.next = lklist[itemsCache.indexOf(newNode.item)];
                    lklist[itemsCache.indexOf(newNode.item)] = newNode;
                    node.children.add(newNode);
                }
                node = node.children.stream().filter(o -> o.item == s.items.get(0)).findFirst().get();
            }
        }

        for(int i = CSupDesc.size()-1; i >= 0; i--) {
            if(CSupDesc.get(i).supCnt >= minSupCnt) {
                freqItemSets.add(CSupDesc.get(i));
            }
            String itm = CSupDesc.get(i).items.get(0);
            TreeNode node = lklist[itemsCache.indexOf(itm)];
            
            // calculate conditional pattern base
            ArrayList<CondPatternItemSet> condPatternBase = new ArrayList<>();
            while(node != null) {
                TreeNode p = node.parent;
                if(p != root) {
                    CondPatternItemSet s = new CondPatternItemSet(node.count);
                    while(true) {  
                        s.items.add(p.item);
                        if(p.parent == root) {
                            s.subTreeId = itemsCache.indexOf(p.item);
                            break;
                        }
                        p = p.parent;
                    }
                    condPatternBase.add(s);
                }
                node = node.next;
            }

            // build conditional FP-tree
            HashMap<Integer, HashMap<String, Integer>> condFpTree = new HashMap<>();
            for(CondPatternItemSet s : condPatternBase) {
                for(String itm1 : s.items) {
                    if(!condFpTree.containsKey(s.subTreeId)) {
                        condFpTree.put(s.subTreeId, new HashMap<>());
                    }
                    if(!condFpTree.get(s.subTreeId).containsKey(itm1)) {
                        condFpTree.get(s.subTreeId).put(itm1, s.supCnt);
                    } else {
                        Integer t = condFpTree.get(s.subTreeId).get(itm1);
                        t += s.supCnt;
                        condFpTree.get(s.subTreeId).put(itm1, t);
                    }
                }
            }
            Iterator it1 = condFpTree.entrySet().iterator();
            while(it1.hasNext()) {
                Map.Entry pair1 = (Map.Entry)it1.next();
                HashMap hm = (HashMap)pair1.getValue();
                Iterator it2 = hm.entrySet().iterator();
                while(it2.hasNext()) {
                    Map.Entry pair2 = (Map.Entry)it2.next();
                    if((int)pair2.getValue() < minSupCnt) {
                        it2.remove();
                    }
                }
                if(hm.isEmpty()) {
                    it1.remove();
                }
            }
            
            // find freq itemsets
            for(HashMap hm : condFpTree.values()) {
                ArrayList<String> arr = new ArrayList<>(hm.keySet());
                ArrayList<String> sub = new ArrayList<>();
                int binSet = (1 << arr.size()) - 1;
                for(int b = binSet; b > 0; b &= binSet) {
                    sub.clear();
                    for(int x = 0; x < arr.size(); x++) {
                        if((b & (1 << x)) > 0) {
                            sub.add(arr.get(x));
                        }
                    }
                    int minCnt = (int)hm.get(sub.get(0));
                    for(int x = 1; x < sub.size(); x++) {
                        if((int)hm.get(sub.get(x)) < minCnt) {
                            minCnt = (int)hm.get(sub.get(x));
                        }
                    }

                    sub.add(itm);
                    sub.sort(new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            return o1.compareTo(o2);
                        }
                    });
                    if(!freqItemSets.stream().filter(o -> o.items.equals(sub)).findFirst().isPresent()) {
                        ItemSet itmSet = new ItemSet(minCnt);
                        itmSet.items = (ArrayList)sub.clone();
                        freqItemSets.add(itmSet);
                    } else {
                        freqItemSets.stream().filter(o -> o.items.equals(sub)).findFirst().get().supCnt += minCnt;
                    }

                    b--;
                }
            }
        }

        System.out.println("INFO: All frequent itemsets:");
        for(ItemSet s : freqItemSets) {
            System.out.println(String.format("  %s support count: %d", s.getItemsRepr(), s.supCnt));
        }

        System.out.println("INFO: All association rules:");
        for(ItemSet s : freqItemSets) {
            // enumerate all non-empty proper subset of s
            int binSet = 0;
            for(String itm : s.items) {
                binSet |= (1 << itemsCache.indexOf(itm));
            }
            int b = binSet;
            while(true) {
                b = (b - 1) & binSet;
                if(b == 0) {
                    break;
                }
                ArrayList<String> subsetItems = new ArrayList<>();
                for(int j = 0; j < itemsCache.size(); j++) {
                    if((b & (1 << j)) > 0) {
                        subsetItems.add(itemsCache.get(j));
                    }
                }
                // s.items & subsetItems are both sorted
                // we can always find the subset
                ItemSet subset = null;
                for(ItemSet s1 : freqItemSets) {
                    if(s1.items.equals(subsetItems)) {
                        subset = s1;
                        break;
                    }
                }
                if((double)s.supCnt / subset.supCnt >= minConfThr) {
                    ArrayList<String> complement = (ArrayList<String>)s.items.clone();
                    complement.removeAll(subset.items);
                    ItemSet dummy = new ItemSet(0);
                    dummy.items = complement;
                    System.out.println(String.format("  %s => %s", subset.getItemsRepr(), dummy.getItemsRepr()));
                }
            }
        }
        return true;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        String defaultDataFilePath = "test-data.txt";
        System.out.println(String.format("Please specify the data file path (default - %s):", defaultDataFilePath));
        String dataFilePath = sc.nextLine();
        if(dataFilePath.isEmpty()) {
            dataFilePath = defaultDataFilePath;
        }
        System.out.println(String.format("INFO: Data file path: %s", dataFilePath));

        int defaultMinSupCnt = 2;
        System.out.println(String.format("Please specify the minimum support count (default - %d):", defaultMinSupCnt));
        int minSupCnt;
        try{
            minSupCnt = Integer.parseInt(sc.nextLine());
        } catch(NumberFormatException e){
            minSupCnt = defaultMinSupCnt;
        }
        System.out.println(String.format("INFO: Minimum support count: %d", minSupCnt));

        double defaultMinConfThr = 0.5;
        System.out.println(String.format("Please specify the minimum confidence threshold (default - %f):", defaultMinConfThr));
        double minConfThr;
        try{
            minConfThr = Double.parseDouble(sc.nextLine());
        } catch(NumberFormatException e){
            minConfThr = defaultMinConfThr;
        }
        System.out.println(String.format("INFO: Minimum confidence threshold: %f", minConfThr));

        fpGrowth(dataFilePath, minSupCnt, minConfThr);

        sc.close();
    }
}
