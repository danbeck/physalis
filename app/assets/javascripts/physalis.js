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

		var android$ = $("[data-platform='android']");
		var ubuntu$ = $("[data-platform='ubuntu']");
		var androidState = android$.attr("data-state");
		var ubuntuState = ubuntu$.attr("data-state");
	
		if (data.android.state !== androidState) {
			updateAndroidDom(node$, data.android);
		}
		if (data.ubuntu.state !== ubuntuState) {
			updateUbuntuDom(node$, data.ubuntu);
		}
	}
}

function updateAndroidDom(node$, data) {
	node$.attr("data-state", data.state);
	node$.empty();
	
	if (data.state === "NEW") {
		node$.append("Android: build queued");
	}
	if (data.state === "IN_PROGRESS") {
		node$.append("Android build in progress");
	}
	if (data.state === "DONE") {
		var html= "<a class='btn' href='"+ data.url + "'>Download Android APK</a>";
		node$.append(html);
	}
}

function updateUbuntuDom(node$, data) {
	node$.attr("data-state", data.state);
	node$.empty();

	if (data.state === "NEW") {
		node$.append("Ubuntu: build queued");
	}
	
	if (data.state === "IN_PROGRESS") {
		node$.append("Ubuntu build in progress");
	}
	if (data.state === "DONE") {
		var html= "<a class='btn' href='"+ data.url + "'>Download Ubuntu binary</a>";
		node$.append(html);
	}
}

$(document).ready(function() {
	function updateTheBuildState() {
		setTimeout(updateTheBuildState, 1000);
		$("div[data-role='downloadProjects']").each(function() {
			retrieveAndAddBuildDataToDom($(this));
		});
	}

//	updateTheBuildState();
});