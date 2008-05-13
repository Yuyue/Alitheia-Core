<%@ page import="eu.sqooss.webui.*" %>
<%

// Display some information about the chose project

/*
if (projectId != null) {
    selectedProject = terrier.getProject(projectId);
}
*/
if (selectedProject != null && selectedProject.isValid()) {
    out.println("<div id=\"selectedproject\" class=\"group\">");
    out.println("<h1>" + selectedProject.getName());
    out.println("<span class=\"forget\"><a href=\"?pid=none\">(forget)</a></span></h1>");

    out.println("\n<h2>Project Metadata</h2>\n");
    out.println(selectedProject.getInfo());

    out.println("\n<h2>Project Versions</h2>\n");
    // Display the number of files in the selected project version
    // TODO: The files number should be cached in the Project object,
    //       instead of calling the Terrier each time.
    //if (selectedProject.getCurrentVersionId() != null) {
        //projectId = selectedProject.getId();
    String inputError = null;
    if (request.getParameter("version" + projectId) != null) {
        Long changeSelected = null;
        try {
            changeSelected =
                new Long(request.getParameter("version" + projectId));
        }
        catch (NumberFormatException e) {
            inputError = new String("Wrong version format!");
        }

        if (changeSelected != null && (changeSelected != selectedProject.getCurrentVersionId())) {
            selectedProject.setCurrentVersionId(changeSelected);
        }
    }
    //out.println("Number of versions: " + selectedProject.countVersions());
    Version currentVersion = selectedProject.getCurrentVersion();
    Long versionNum = currentVersion.getNumber();
    Long versionId = currentVersion.getId();
    // Display the first and last known project versions
    if (selectedProject.getFirstVersion() != null) {
        if (selectedProject.getFirstVersion()
            != selectedProject.getLastVersion()) {
            out.println ("<br />Versions: "
                + selectedProject.getFirstVersion().getId()
                + " - "
                + selectedProject.getLastVersion().getId()
                + " (" + selectedProject.countVersions() + " total)");
        } else {
            out.println ("<br />Version: "
                + selectedProject.getFirstVersion());
        }
    } else {
        out.println("Project doesn't seem to have versions recorded.");
    }
    out.println (versionSelector(projectId, versionId));
    if (inputError != null) {
        out.println(Functions.error(inputError));
    }

    out.println("<h2>Metrics for project " + selectedProject.getName() + "</h2>");
    out.println(selectedProject.showMetrics());

    String versionFileList = selectedProject.getCurrentVersion().listFiles();
    String projectFileList = selectedProject.listFiles();
    int v_c = selectedProject.getCurrentVersion().getFileCount();
    int p_c = selectedProject.getFileCount();

    out.println ("<br /><h2>Files</h2> "
        + v_c + " file(s) in version " + selectedProject.getCurrentVersionId());

    out.println("\n<table width=\"100%\">\n\t<tr><td>");

    out.println("<strong>Files in " + selectedProject.getName() + " (" + p_c + ")</strong>");
    out.println(projectFileList);

    out.println("\n\t\t</td><td>");

    out.println("<strong>Files in Version " + selectedProject.getCurrentVersionId() + " (" + v_c + ")</strong>");
    out.println(versionFileList);

    out.println("\n\t\t</td>\n\t</tr>\n</table>");

    out.println("</div>"); // End of this group
    out.println("<div style=\"margin-bottom: 20px;\"></div>");
} else {
    //out.println("Project apparently null");
}

%>











