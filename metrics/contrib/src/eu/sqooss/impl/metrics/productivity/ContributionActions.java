/*
 * This file is part of the Alitheia system, developed by the SQO-OSS
 * consortium as part of the IST FP6 SQO-OSS project, number 033331.
 *
 * Copyright 2008 Athens University of Economics and Business
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package eu.sqooss.impl.metrics.productivity;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Typesafe descriptions of actions supported by the productivity plugin
 *
 */
public class ContributionActions {

    /** Maps categories to actions*/
    public static HashMap<ActionCategory, ArrayList<ActionType>> types = 
        new java.util.HashMap<ActionCategory, ArrayList<ActionType>>();
    
    static {
        ArrayList<ActionType> c = new ArrayList<ActionType>();
        c.add(ActionType.CNS);
        c.add(ActionType.CND);
        c.add(ActionType.CDF);
        c.add(ActionType.CTF);
        c.add(ActionType.CBF);
        c.add(ActionType.CEC);
        c.add(ActionType.CMF);
        c.add(ActionType.CBN);
        c.add(ActionType.CPH);
        c.add(ActionType.CAL);
        types.put(ActionCategory.C, c);
    }
    
    /**
     * A basic categorization of the all the possible actions on
     * various project assets
     */
    public enum ActionType {
        /** Commit new source file */
        CNS,
        /** Commit new directory */
        CND,
        /** Commit documentation files */
        CDF,
        /** Commit translation files */
        CTF,
        /** Commit binary files */
        CBF,
        /** Commit with empty commit message */
        CEC,
        /** Commit more than X files in a single commit */
        CMF,
        /** 
         * Commit to the SCM repository (for calculating the number
         * of commits per developer) 
         */
        TCO,
        /**
         * Commit files (for calculating the number of committed files per
         * developer)
         */
        TCF,
        /** Commit comment that includes a bug report number */
        CBN,
        /** Commit comment that awards a pointy hat */
        CPH,
        /** Add or remove lines of code */
        CAL;
        
        public static ActionType fromString(String s) {
            if ("CNS".equalsIgnoreCase(s))
                return ActionType.CNS;
            else if ("CND".equalsIgnoreCase(s))
                return ActionType.CND;
            else if ("CDF".equalsIgnoreCase(s))
                return ActionType.CDF;
            else if ("CTF".equalsIgnoreCase(s))
                return ActionType.CTF;
            else if ("CBF".equalsIgnoreCase(s))
                return ActionType.CBF;
            else if ("CEC".equalsIgnoreCase(s))
                return ActionType.CEC;
            else if ("CMF".equalsIgnoreCase(s))
                return ActionType.CMF;
            else if ("TCO".equalsIgnoreCase(s))
                return ActionType.TCO;
            else if ("TCF".equalsIgnoreCase(s))
                return ActionType.TCF;
            else if ("CBN".equalsIgnoreCase(s))
                return ActionType.CBN;
            else if ("CPH".equalsIgnoreCase(s))
                return ActionType.CPH;
            else if ("CAL".equalsIgnoreCase(s))
                return ActionType.CAL;
            else
                return null;
        }
        
        public static ArrayList<ActionType> getActionTypes(ActionCategory a){
            return types.get(a);
        }
    }
    
    /**
     * An action can fall into in one of those categories
     */
    public enum ActionCategory{
        /** Code and documentation repository */
        C,
        /** Mailing lists - forums */
        M,
        /** Bug database*/
        B;  
        
        public static ActionCategory fromString(String s){
            if ("C".equalsIgnoreCase(s))
                return ActionCategory.C;
            else if ("M".equalsIgnoreCase(s))
                return ActionCategory.M;
            else if ("B".equalsIgnoreCase(s))
                return ActionCategory.B;
            else
                return null;
        }
        
        public static ActionCategory getActionCategory(ActionType a){
            for(ActionCategory ac : types.keySet()) {
                for(ActionType at : types.get(ac)) {
                    if (at.equals(a)) {
                        return ac;
                    }
                }
            }
            return null;
        }
    }
}

//vi: ai nosi sw=4 ts=4 expandtab