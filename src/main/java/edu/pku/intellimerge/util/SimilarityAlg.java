package edu.pku.intellimerge.util;

import java.util.HashSet;
import java.util.Set;

public class SimilarityAlg {

    public static double jaccard(Set s1, Set s2){
        Set<String> union = new HashSet<>();
        union.addAll(s1);
        union.addAll(s2);
        System.out.println(union);
        Set<String> intersection = new HashSet<>();
        intersection.addAll(s1);
        intersection.retainAll(s2);
        System.out.println(intersection);

        return (double) intersection.size() / union.size();
    }
}
