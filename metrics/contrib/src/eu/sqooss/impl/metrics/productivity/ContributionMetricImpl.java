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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;

import eu.sqooss.core.AlitheiaCore;
import eu.sqooss.impl.metrics.productivity.ContributionActions.ActionCategory;
import eu.sqooss.impl.metrics.productivity.ContributionActions.ActionType;
import eu.sqooss.metrics.productivity.ContributionMetric;
import eu.sqooss.metrics.productivity.db.ContribActionType;
import eu.sqooss.metrics.productivity.db.ContribAction;
import eu.sqooss.metrics.productivity.db.ContribActionWeight;
import eu.sqooss.service.abstractmetric.AbstractMetric;
import eu.sqooss.service.abstractmetric.AlitheiaPlugin;
import eu.sqooss.service.abstractmetric.MetricMismatchException;
import eu.sqooss.service.abstractmetric.ResultEntry;
import eu.sqooss.service.db.DAObject;
import eu.sqooss.service.db.Developer;
import eu.sqooss.service.db.Metric;
import eu.sqooss.service.db.MetricType;
import eu.sqooss.service.db.PluginConfiguration;
import eu.sqooss.service.db.ProjectFile;
import eu.sqooss.service.db.ProjectVersion;
import eu.sqooss.service.db.StoredProject;
import eu.sqooss.service.fds.FileTypeMatcher;
import eu.sqooss.service.pa.PluginInfo;

public class ContributionMetricImpl extends AbstractMetric implements
        ContributionMetric {

    public static final String CONFIG_CMF_THRES = "CMF_threshold";
    public static final String CONFIG_WEIGHT_UPDATE_VERSIONS = "Weights_Update_Interval";
    
    public ContributionMetricImpl(BundleContext bc) {
        super(bc);
        super.addActivationType(ProjectVersion.class);
        super.addActivationType(Developer.class);
        
        super.addMetricActivationType("PROD", Developer.class);
        
        super.addDependency("Wc.loc");
    }
    
    public boolean install() {
    	 boolean result = super.install();
         if (result) {
             result &= super.addSupportedMetrics(
                     "Developer Contribution Metric",
                     "CONTRIB",
                     MetricType.Type.PROJECT_WIDE);
         
             addConfigEntry(CONFIG_CMF_THRES, 
                 "5" , 
                 "Number of committed files above which the developer is penalized", 
                 PluginInfo.ConfigurationType.INTEGER);
             addConfigEntry(CONFIG_WEIGHT_UPDATE_VERSIONS, 
                 "150" , 
                 "Number of revisions between weight updates", 
                 PluginInfo.ConfigurationType.INTEGER);
         }
         return result;
    }
    
    public boolean remove() {
        boolean result = true;
        
        String[] tables = {"ProductivityWeights", "ProductivityActions",
                "ProductivityActionType"};
        
        for (String tablename : tables) {
            result &= db.deleteRecords((List<DAObject>) db.doHQL("from " + tablename));
        }
        
        result &= super.remove();
        return result;
    }
    
    public boolean cleanup(DAObject sp) {
        boolean result = true;
        
        if (!(sp instanceof StoredProject)) {
            log.warn("We only support cleaning up per stored project for now");
            return false;
        }
        
        Map<String,Object> params = new HashMap<String,Object>();
        List<ProjectVersion> pvs = ((StoredProject)sp).getProjectVersions();
        
        for(ProjectVersion pv : pvs) {
            params.put("projectVersion", pv);
            List<ContribAction> pas = 
                db.findObjectsByProperties(ContribAction.class, params);
            if (!pas.isEmpty()) {
                for (ContribAction pa : pas) {
                    result &= db.deleteRecord(pa);
                }
            }
            params.clear();
        }
        
        return result;
    }

    /**
     * Returns an arbitrary result to indicate that the provided project
     * version has been already processed. If the provided version 
     * was not processed, it returns null.
     * 
     * {@inheritDoc}
     */
    public List<ResultEntry> getResult(ProjectVersion a, Metric m) {
        
        ArrayList<ResultEntry> res = new ArrayList<ResultEntry>();
        String paramVersion = "paramVersion";
        
        String query = "select a from ProductivityActions a " +
                " where a.projectVersion = :" + paramVersion ;
        
        Map<String,Object> parameters = new HashMap<String,Object>();
        parameters.put(paramVersion, a);

        List<?> p = db.doHQL(query, parameters);
    
        if ( p == null || p.isEmpty() ){
            return null;
        } 
        // TODO: Fix the fixed result
        res.add(new ResultEntry(1, ResultEntry.MIME_TYPE_TYPE_INTEGER, 
                m.getMnemonic()));
        return res;
    }

    /**
     * This plug-in's result is returned per developer. 
     */
    public List<ResultEntry> getResult(Developer a, Metric m) {
        
        ArrayList<ResultEntry> results = new ArrayList<ResultEntry>();
        ContribActionWeight weight;
        double value = 0;

        ActionCategory[] actionCategories = ActionCategory.values();

        for (int i = 0; i < actionCategories.length; i++) {
            weight = ContribActionWeight.getWeight(actionCategories[i]);

            if (weight != null) {
                value = value + weight.getWeight() * 
                    getResultPerActionCategory(a, actionCategories[i]);
            }
        }

        ResultEntry entry = new ResultEntry(value,
                ResultEntry.MIME_TYPE_TYPE_DOUBLE, m.getMnemonic());
        results.add(entry);
        return results;
    }

    public void run(ProjectVersion pv) {
        /* Read config options in advance*/        
        FileTypeMatcher.FileType fType;
        ProjectFile prevFile;
        int locCurrent, locPrevious;
        Developer dev = pv.getCommitter();
        String commitMsg = pv.getCommitMsg();
        Set<ProjectFile> projectFiles = pv.getVersionFiles();
        
        List<Metric> locMetric = new ArrayList<Metric>();
        AlitheiaPlugin plugin = AlitheiaCore.getInstance().getPluginAdmin().getImplementingPlugin("Wc.loc");
        
        if (plugin != null) {
            locMetric = plugin.getSupportedMetrics();
        } else {
            log.error("Could not find the WC plugin");
            return;
        }
        
        PluginConfiguration pluginConf = getConfigurationOption(
                ContributionMetricImpl.CONFIG_CMF_THRES);
        
        if (pluginConf == null || 
                Integer.parseInt(pluginConf.getValue()) <= 0) {
            log.error("Plug-in configuration option " + 
                    ContributionMetricImpl.CONFIG_CMF_THRES + " not found");
            return; 
        }
        
        Pattern bugNumberLabel = Pattern.compile("\\A.*(pr:|bug:).*\\Z",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        Pattern pHatLabel = Pattern.compile(
                "\\A.*(ph:|pointy hat|p?hat:).*\\Z", Pattern.CASE_INSENSITIVE
                        | Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m;

        if (commitMsg.length() == 0) {
            updateField(pv, dev, ActionType.CEC, false, 1);
        } else {
            m = bugNumberLabel.matcher(commitMsg);
            if (m.matches()) {
                updateField(pv, dev, ActionType.CBN, true, 1);
            }
            m = pHatLabel.matcher(commitMsg);
            if (m.matches()) {
                updateField(pv, dev, ActionType.CPH, true, 1);
            }
        }
      
        if (projectFiles.size() > Integer.parseInt(pluginConf.getValue())) {
            updateField(pv, dev, ActionType.CMF, false, 1);
        }

        Iterator<ProjectFile> i = projectFiles.iterator();
        
        while (i.hasNext()) {
            ProjectFile pf = i.next();

            fType = FileTypeMatcher.getFileType(pf.getFileName());

            if (pf.getIsDirectory()) {
                //New directory added
                if (pf.isAdded()) {
                    updateField(pv, dev, ActionType.CND, true, 1);
                }
            } else if (fType == FileTypeMatcher.FileType.SRC) {
                //Source file change, calc number of lines commited
                try {
                    //File deleted, set current lines to 0 
                    if (pf.isDeleted()) {
                        locCurrent = 0;
                    } else {
                        //Get lines of current version of the file from the wc metric
                        locCurrent = plugin.getResult(pf, locMetric).getRow(0).get(0).getInteger();
                    }
                    //Source file just added
                    if (pf.isAdded()) {
                        updateField(pv, dev, ActionType.CNS, true, 1);
                        locPrevious = 0;
                    } else {
                        //Existing file, get lines of previous version
                        prevFile = ProjectFile.getPreviousFileVersion(pf);
                        if (prevFile != null) {
                            locPrevious = plugin.getResult(prevFile, locMetric).getRow(0).get(0).getInteger();
                        } else {
                            log.warn("Cannot get previous file " +
                                        "version for file id: " + pf.getId());
                            locPrevious = 0;
                        }
                    }
                    updateField(pv, dev, ActionType.CAL, true, abs(locCurrent - locPrevious));
                } catch (MetricMismatchException e) {
                    log.error("Results of LOC metric for project: "
                            + pv.getProject().getName() + " file: "
                            + pf.getFileName() + ", Version: "
                            + pv.getRevisionId() + " could not be retrieved: "
                            + e.getMessage());
                    return;
                }
            } else if (fType == FileTypeMatcher.FileType.BIN) {
                updateField(pv, dev, ActionType.CBF, true, 1);
            } else if (fType == FileTypeMatcher.FileType.DOC) {
                updateField(pv, dev, ActionType.CDF, true, 1);
            } else if (fType == FileTypeMatcher.FileType.TRANS) {
                updateField(pv, dev, ActionType.CTF, true, 1);
            }
        }
        
        //Check if it is required to update the weights
        pluginConf = getConfigurationOption(
                ContributionMetricImpl.CONFIG_WEIGHT_UPDATE_VERSIONS);
        
        if (pluginConf == null || 
                Integer.parseInt(pluginConf.getValue()) <= 0) {
            log.error("Plug-in configuration option " + 
                    ContributionMetricImpl.CONFIG_WEIGHT_UPDATE_VERSIONS + " not found");
            return;
        }
        
        synchronized(getClass()){
            //long distinctVersions = calcDistinctVersions();
            long ts = (System.currentTimeMillis()/1000);
                long previousVersions = ContribActionWeight.getLastUpdateVersionsCount();
            //Should the weights be updated?
            if (ts - previousVersions 
                    >= Integer.parseInt(pluginConf.getValue())){
                updateWeights(ts);
            }
        }
        markEvaluation(Metric.getMetricByMnemonic("PROD"), pv.getProject());
    }

    public void run(Developer v) {
        
    }

    /**
     * Get result per developer and per category
     * 
     */
    private double getResultPerActionCategory(Developer d, ActionCategory ac) {
        
        ArrayList<ActionType> actionTypes = ActionType.getActionTypes(ac);
        
        ContribActionWeight weight;
        long totalActions;
        double value = 0;

        for (int i=0; i<actionTypes.size(); i++) {
            weight = ContribActionWeight.getWeight(actionTypes.get(i));
            
            if (weight == null) {
                continue;
            }
            
            ContribActionType at = 
                ContribActionType.getProductivityActionType(actionTypes.get(i), null);
                
            totalActions = 
                ContribAction.getTotalActionsPerTypePerDeveloper(actionTypes.get(i), d);

            if(totalActions != 0){
                if (at.getIsPositive())
                    value += weight.getWeight() * totalActions;
                else
                    value -= weight.getWeight() * totalActions;
            }
        }
        return value;
    }
    
    private long calcDistinctVersions() {
        List<?> distinctVersions = db.doHQL("select " +
                        "count(distinct projectVersion) from ProductivityActions");
        
        if(distinctVersions == null || 
                distinctVersions.size() == 0 || 
                distinctVersions.get(0) == null) {
            return 0L;
        }
        
        return (Long.parseLong(distinctVersions.get(0).toString())) ;
    }
    
    private void updateField(ProjectVersion pv, Developer dev, ActionType actionType,
            boolean isPositive, int value) {
       
        ContribActionType at = ContribActionType.getProductivityActionType(actionType, isPositive);
        
        if (at == null){
            db.rollbackDBSession();
            return;
        }
                
        ContribAction a = ContribAction.getProductivityAction(dev, pv, at);

        if (a == null) {
            a = new ContribAction();
            a.setDeveloper(dev);
            a.setProjectVersion(pv);
            a.setProductivityActionType(at);
            a.setTotal(value);
            db.addRecord(a);
        } else {
            a.setTotal(a.getTotal() + value);
        }
    }
    
    private void updateWeights(long secLastUpdate) {
        ActionCategory[] actionCategories = ActionCategory.values();

        long totalActions = ContribAction.getTotalActions();
        long totalActionsPerCategory;
        long totalActionsPerType;
        
        if (totalActions <= 0) {
            return;
        }
        
        for (int i = 0; i < actionCategories.length; i++) {
            //update action category weight
            totalActionsPerCategory = 
                ContribAction.getTotalActionsPerCategory(actionCategories[i]);
                
            if (totalActionsPerCategory <= 0) {
                continue;
            }
            
            updateActionCategoryWeight(actionCategories[i],
                    totalActionsPerCategory, totalActions, secLastUpdate);

            // update action types weights
            ArrayList<ActionType> actionTypes = 
                ActionType.getActionTypes(actionCategories[i]);

            for (int j = 0; j < actionTypes.size(); j++) {
                totalActionsPerType = 
                    ContribAction.getTotalActionsPerType(actionTypes.get(j));
                updateActionTypeWeight(actionTypes.get(j),totalActionsPerType, 
                        totalActionsPerCategory, secLastUpdate);
            }
        }
    }
    
    private void updateActionTypeWeight(ActionType actionType, 
            long totalActionsPerType, long totalActionsPerCategory, 
            long distinctVersions) {

        double weight = (double)(100 * totalActionsPerType) / 
            (double)totalActionsPerCategory;

        ContribActionWeight a = ContribActionWeight.getWeight(actionType);
       
        if (a == null) {
            a = new ContribActionWeight();
            a.setType(actionType);
            a.setWeight(weight);
            a.setLastUpdateVersion(distinctVersions);
            db.addRecord(a);
        } else {
            a.setLastUpdateVersion(distinctVersions);
            a.setWeight(weight);
        }
    }
    
    private void updateActionCategoryWeight(ActionCategory actionCategory, 
            long totalActionsPerCategory, long totalActions, 
            long distinctVersions){

        double weight = (double)(100 * totalActionsPerCategory) / 
            (double)totalActions;

        ContribActionWeight a = ContribActionWeight.getWeight(actionCategory);

        if (a == null) { //No weight calculated for this action yet
            a = new ContribActionWeight();
            a.setCategory(actionCategory);
            a.setWeight(weight);
            a.setLastUpdateVersion(distinctVersions);
            db.addRecord(a);
        } else {
            a.setLastUpdateVersion(distinctVersions);
            a.setWeight(weight);
        }
    }
    
    private int abs (int value){
        if (value < 0) 
            return -1 * value;
        else
            return value;
    }
    
}

// vi: ai nosi sw=4 ts=4 expandtab