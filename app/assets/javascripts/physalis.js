function retrieveAndAddBuildDataToDom(node$) {
	var projectname = node$.attr("data-project");
	var username = node$.attr("data-user");

	var result = {};
	$.getJSON("https://physalis.io/latest/" + username + "/" + projectname, function(data) {
		updateDom(node$, data);
	});
}

function updateDom(node$, data) {
	if (!data.error) {

		node$.empty();
		updateDomForPlatform(node$,data.ubuntu, "ubuntu");
		updateDomForPlatform(node$,data.android, "android");
		updateDomForPlatform(node$,data.firefoxos, "firefoxos");
	}
}

function updateDomForPlatform(node$,  data, platform) {
	var html ="";
	var htmlLog = "";
	
	if (data.state === "NEW") {
		html = template_newArtifact(platform);
	}
	
	if (data.state === "IN_PROGRESS") {
		html = template_artifactInProgress(platform);
	}
	
	if (data.state === "DONE") {
			html = template_artifactDone(platform,data);
			htmlLog = template_logDone(platform, data);
	}
	
	if (data.state === "ERROR") {
    html = template_artifactError(platform);
		htmlLog = template_logError(platform,data);
	}
	node$.append(html + htmlLog);
}

function template_newArtifact(platform) {
	return "<div class='col-xs-8 bg-info' data-type='artifact' data-state='NEW' data-platform='" + platform + "'>"+
         platform + ": build queued" +
         "</div>";
}

function template_artifactInProgress(platform) {
	return " <div class='bg-info col-xs-8' data-type='artifact' data-state='IN_PROGRESS' data-platform='"+ platform + "'>"+
  "<i class='fa fa-circle-o-notch fa-spin'></i> <span>" + platform +": build in progress</span></div>";
}

function template_artifactDone(platform, data) {
	return "<div class='col-xs-8'  data-type='artifact' data-state='DONE' data-platform='" + platform +"'>"+
  "<a class='btn btn-primary btn-block' role='button'  href='" + data.url + "'>" +
  "<i class='fa fa-download'></i> " + platform + 
  "</a></div>";

}

function template_artifactError(platform){
	return "<div class='bg-danger col-xs-8' data-type='artifact' data-state='ERROR' data-platform='" + platform  + "'>" +
  "<span class='glyphicon glyphicon-exclamation-sign'></span>&nbsp;<span>" + platform + ": build failed</span>"+
  "</div>";
}

function template_logError(platform,data){
   return "<div class='col-xs-4' data-platform-log='" + platform + "'>" +
  "<a target='_blank' class='btn btn-build btn-block' role='button'  href='" + data.logurl + "'><i class='fa fa-file-text'></i> Logs</a>" +
  "</div>";
}

function template_logDone(platform,data){
	return "<div class='col-xs-4' data-platform-log='"+ platform +"'>" +
  "<a target='_blank' class='btn btn-build btn-block' role='button'  href='" + data.logurl + "'><i class='fa fa-file-text'></i> Logs</a>" +
  "</div>";
}

$(document).ready(function() {
	function updateTheBuildState() {
		setTimeout(updateTheBuildState, 3500);
		$("div[data-role='downloadProjects']").each(function() {
			retrieveAndAddBuildDataToDom($(this));
		});
	}

	updateTheBuildState();
});