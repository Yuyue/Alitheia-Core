var prefix = "/web";

/* Local object cache, gets built gradually as AJAX requests return*/
var state = {
    metrics : ''
};

//Javascript entry point
$(document).ready(function() {
    getProjects();
    $("#projectSelect").change(projectSelected);
    state.metrics = new Array();
    getMetrics('SOURCE_FILE');
    getMetrics('SOURCE_DIRECTORY');
    getMetrics('PROJECT_VERSION');
});

function getProjects() {
  $.ajax( {
    url : prefix + "/proxy/project",
    dataType : 'json',
    type : 'GET',
    success : function(data) {
      $.each(data, function(i, obj) {
        $("<option/>").attr("value", obj.project.id).text(obj.project.name)
            .appendTo("#projectSelect");
      });
    },
    error : function(xhr, status, error) {
      alert("No projects found");
    }
  }); 
}

function projectSelected() {
  cleanup();
  state.smplsize = '';
  state.prid = '';
  state.minVer = '';
  state.maxVer = '';
  state.verLatest = '';
  state.verFirst = '';
  state.selVersion = '';
  state.dirs = '';
  state.files = '';
  state.results = '';
  state.versionsCache = '';
  
  state.prid = $("#projectSelect option:selected").val();
  
  if (state.prid == "-")
    return;

  $("#projectName").text($("#projectSelect option:selected").text());
  
  $.getJSON(prefix + '/proxy/project/' + state.prid + "/version/latest",
      gotLatestVersion);
}

function gotLatestVersion (data) {
  state.verLatest = data;
  getDirs(state.verLatest, displayDirs);
  $.getJSON(prefix + '/proxy/project/' + state.prid + "/version/first", 
      gotFirstVersion);
}

function gotFirstVersion (data) {
  state.verFirst = data;
  displayVersions();
}

//Cleanup and re-initialise controls
function cleanup() {
  $("#verplot2metr").empty();
  $("#verplot1metr").empty();
  $('#verplot1plot').empty();
  $('#verplot2plot').empty();
  $("#verplot1metr").change();
  $("#verplot2metr").change();
  $("#dirs thead tr").empty();
  $("#dirs tbody").empty();
  $("#files thead tr").empty();
  $("#files tbody").empty();
}

function displayVersions() {
  //Fill in metric selectors
  $.each(state.metrics['PROJECT_VERSION'], function(i, obj){
    $("<option/>").attr("value", obj.metric.id).text(obj.metric.mnemonic 
        + " | " + obj.metric.description).appendTo("#verplot2metr");
    $("<option/>").attr("value", obj.metric.id).text(obj.metric.mnemonic 
        + " | " + obj.metric.description).appendTo("#verplot1metr");
  });
  
  //Setup triggers for metric selectors
  $("#verplot1metr").change(loadVerPlot1);
  $("#verplot2metr").change(loadVerPlot2);

  //Setup version slider
  $("#versionrange").slider({
    range: true,
    min: state.verFirst.version.revisionId,
    max: state.verLatest.version.revisionId,
    values: [0, state.verLatest.version.revisionId],
    stop: function(event, ui) {
      $("#versionlbl").val(ui.values[0] + '-' + ui.values[1]);
      state.minVer = ui.values[0];
      state.maxVer = ui.values[1];
      state.selVersion =  state.maxVer;
      verReplot();
      getDirs(state.selVersion, displayDirs);
    }
  });
  $("#versionlbl").val(state.verFirst.version.revisionId + ' - ' + state.verLatest.version.revisionId);
  
  //Setup the sample size label
  $("#samplesize").slider({
    min: 0,
    max: 50,
    values: [25],
    stop: function(event, ui) {
      $("#smpllbl").val(ui.values[0]);
      state.smplsize = ui.values[0];
      verReplot();
    }
  });
  $("#smpllbl").val($("#samplesize").slider("values", 0));
  state.smplsize = $("#samplesize").slider("values", 0);
  
  //Show the version pane
  if ($("#versionpane").is(":visible") == false)
    $("#versionpane").show('clip', null, 500);
  
  //Trigger metric a metric selection, to start metric results download
  verReplot();
}

function verReplot() {
  loadVerPlot1();
  loadVerPlot2();
}

function loadVerPlot1() {
  $('#verplot1plot').empty();
  
  var revs = getVersionNumbers(
      state.smplsize,
      state.verFirst.version.revisionId, 
      state.verLatest.version.revisionId);
  
  getVersions(revs, ver1Plot);
 }

function ver1Plot(revs) {
  var versions = new Array();
  $.each(revs, function(i, rev) { 
    if (versionIdFromRevision (rev) != null)
      versions.push(versionIdFromRevision (rev));
  });
  
  getResults(versions, $('#verplot1metr').val(), plotVerPlot, 
      {plot: 'verplot1plot', ylabel: $('#verplot1metr :selected').text(), revisionIds: revs});
}

function loadVerPlot2() {
  $('#verplot2plot').empty();
  
  var revs = getVersionNumbers(
      state.smplsize,
      state.verFirst.version.revisionId, 
      state.verLatest.version.revisionId);
  
  getVersions(revs, ver2Plot); 
}

function ver2Plot(revs) {
  var versions = new Array();
  $.each(revs, function(i, rev) { 
    if (versionIdFromRevision (rev) != null)
      versions.push(versionIdFromRevision (rev));
  });
  
  getResults(versions, $('#verplot2metr').val(), plotVerPlot, 
      {plot: 'verplot2plot', ylabel: $('#verplot2metr :selected').text(), revisionIds: revs});
}

function versionIdFromRevision (rev) {
  if (state.versionsCache[rev] == null)
    return null;
  
  return state.versionsCache[rev].id;
}

//Plot a project version metric
function plotVerPlot(metric, args) {
  var points = [];
  $.each(args.revisionIds, function(i, data) {
    var result = getResultFromCache(data, metric);
    if (result != null)
      points.push([data, result.result]);
  });

  $.jqplot(args.plot, [points],  {
    axes:{
      xaxis:{
        label:'Version',
        min: 0
      },
      yaxis:{
        label: args.ylabel,
        autoscale: true,
        min: 0
      }
    }
  });
}

function getResultFromCache(resourceId, metricId) {
  
  if (state.versionsCache[resourceId] == null)
    return null;
  
  var list = state.byResourceCache[resourceId];
  
  if (list == null)
    return null;
  
  var result = null;
  $.each(list, function(i, data){
    if (data.r.metricId == metricId)
      result = data.r;
  });
  return result;
}

/*Metrics helpers*/
/*Get and cache a list of metrics by type*/
function getMetrics(type) {
  $.ajax({
    url : prefix + "/proxy/metrics/by-type/" + type,
      dataType : 'json',
      type : 'GET',
      success : function(data) {
        state.metrics[type] = data;
      },
      error : function(xhr, status, error) {
        //alert("No metrics of type " + type);
      }
  });
}

/*Get and cache metric results by resource and metric id*/
function getResults(resourceIds, metricId, callback, callbackargs) {
  var resources = new String();
  
  if (state.byMetricCache == null)
    state.byMetricCache = new Array();
  if (state.byResourceCache == null)
    state.byResourceCache = new Array();
 
   $.each(resourceIds, function(i, val) {
     //Don't retrieve stuff that is cached
     if (getResultFromCache(val, metricId) == null) {
       resources = resources + val + ",";
     }
   });
 
   if (resources == "") {
     if (callback != null) callback();
     return;
   }
   
  $.ajax({
    url : prefix + "/proxy/metrics/by-id/" + metricId 
      + "/result/" + resources,
      dataType : 'json',
      type : 'GET',
      success : function(data) {
       
        $.each(data, function(i, val) {
          if (state.byMetricCache[metricId] == null)
            state.byMetricCache[metricId] = new Array();
          state.byMetricCache[metricId].push(val);
          
          if (state.byResourceCache[val.r.artifactId] == null)
            state.byResourceCache[val.r.artifactId] = new Array();
          
          state.byResourceCache[val.r.artifactId].push(val);
        });
        if (callback != null) callback(metricId, callbackargs);
      },
      error : function(xhr, status, error) {
        alert("No metrics found " + error);
      }
  });
}

/*Get an array of a maximum num evenly distributed numbers between min and max*/
function getVersionNumbers(num, min, max) {
  var result = new Array();
  
  if (max - min <= num) {
    var i;
    for (i = min; i <= max; i++)
      result.push(i);
    return result;
  }
  
  if (max - min <= 0)
    return result;
  
  var incr = (max - min)/num;
  
  for (i = min; i <= max; i += incr)
    result.push(Math.floor(i));
  
  return result;
}

function getVersions(versions, callback) {
  var resources = "";
  
  if (state.versionsCache == '')
    state.versionsCache = new Array();

  $.each(versions, function(i, val) {
    //Don't retrieve stuff that is cached
    if (state.versionsCache[val] == null) 
      resources = resources + val + ",";
  });

  if (resources == "") {
    if (callback != null) callback(versions);
    return;
  }
  
  $.ajax({
    url : prefix + "/proxy/project/" 
      + state.prid + "/versions/" + resources,
      dataType : 'json',
      type : 'GET',
      success : function(data) {
        $.each(data, function(i, val) {
          state.versionsCache[val.version.revisionId] = val.version;
        });
        if (callback != null) callback(versions);
      },
      error : function(xhr, status, error) {
        alert("No metrics found " + error);
      }
  }); 
}

/*File view*/
/*Get a list of all directories and cache them locally*/
function getDirs(version, callback) {
	if (state.selVersion != '' &&
			state.selVersion.version.id == version.version.id) {
		if (callback != null) callback();
		return;
	}
	
	$.ajax({
		url : prefix + "/proxy/project/" 
			+ state.prid + "/version/" 
			+ version.version.revisionId + "/dirs/",
	    dataType : 'json',
	    type : 'GET',
	    success : function(data) {
	      state.dirs = new Array(); state.files = new Array();
			
			  //Index dirs by path
		    data.forEach(function(f) {
		      state.dirs.push(f.file);
		    });
		    
		    state.selVersion = version;
		    if (callback != null) callback();
	    },
	    error : function(xhr, status, error) {
	    	alert("No directories found for version: " + version);
	    }
	});
}

function displayDirs() {
  //Clean up old table contents
	$("#dirs thead tr").empty();
	$("#dirs tbody").empty();
	
	//Print the headers
	$("#dirs thead tr").append("<td>Directories</td>");
	$.each(state.metrics['SOURCE_DIRECTORY'], function(i, obj) {
		$("#dirs thead tr").append("<td>" + obj.metric.mnemonic + "</td>");
	});
	
	//Print the directories
	$.each(state.dirs, function(i, file) {
	  var name = getFilePath(file);
		$("#dirs tbody").append($("<tr/>").attr("id", name));
		$("#dirs tr[id="+ name + "]").append("<td class=\"dir\">" + name + "</td>");
		
		//Placeholders for metrics
		$.each(state.metrics['SOURCE_DIRECTORY'], function(j, obj) {
			$("#dirs tr[id=" + name + "]").append("<td></td>");
		});
	});
	
	//Add a mouse over effect
	$("#dirs tbody tr td").mouseover(function(){
		$(this).addClass("fileover");
	}).mouseout(function(){
		$(this).removeClass("fileover");
	});
	
	//Add a click event, show the files
  $("#dirs .dir").click(function(){
    $(".dirclicked").removeClass("dirclicked");
    $(this).parent().addClass("dirclicked");
    getFiles($(this).parent().attr("id"), displayFiles);
  });
  
  if ($("#filepane").is(":visible") == false)
    $("#filepane").show('clip', null, 500);
}

function getFilePath(file) {
  if (file.dir.path == "/")
    return "/" + file.name;
  return file.dir.path + "/" + file.name;
}

/* Retrieve files for a dir, cache them and call a function if successful*/
function getFiles(dir, callback) {
  
  if (state.files[dir] != null 
      && state.files[dir].length > 0) {
    if (callback != null) callback(dir);
    return;
  }
  
  $.ajax({
    url : prefix + "/proxy/project/" 
      + state.prid + "/versions/" 
      + state.selVersion.version.revisionId + "/files" + dir,
      dataType : 'json',
      type : 'GET',
      success : function(data) {
        //Index files by including dir path
        data.forEach(function(f) {
          if (state.files[dir] == null)
            state.files[dir] = new Array();
          
          state.files[dir].push(f.file);
        });
        
        if (callback != null) callback(dir);
      },
      error : function(xhr, status, error) {
        alert("No files found for dir: " + dir);
      }
  });
}

function displayFiles(dir) {
//Clean up old table contents
  $("#files thead tr").empty();
  $("#files tbody").empty();
  
  //Print the headers
  $("#files thead tr").append("<td>Files</td>");
  $.each(state.metrics['SOURCE_FILE'], function(i, obj) {
    $("#files thead tr").append("<td>" + obj.metric.mnemonic + "</td>");
  });

  for (var i in state.files[dir]) {
    $("#files tbody").append($("<tr/>").attr("id", i));
    $("#files tr[id="+ i + "]").append("<td class=\"file\">" + state.files[dir][i].name + "</td>");
    
    //Placeholders for metrics
    $.each(state.metrics['SOURCE_DIRECTORY'], function(j, obj) {
      $("#dirs tr[id=" + i + "]").append("<td></td>");
    });
  }
}