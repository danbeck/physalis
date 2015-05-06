$(document).ready(
		function() {

			function retrieveAndAddBuildDataToDom(node$) {
				var projectname = node$.attr("data-project");
				var username = node$.attr("data-user");

				var result = {};
				$.getJSON("latest/" + username + "/" + projectname, function(
						data) {
					if (!data.error) {
						node$.empty();
						if (data.android.state === "NEW") {
							node$.append("<p>Android: build queued</p>");
						}
						if (data.ubuntu.state === "NEW") {
							node$.append("<p>Ubuntu: build queued</p>");
						}
						if (data.android.state === "IN_PROGRESS") {
							node$.append("<p>Android build in progress</p>");
						}
						if (data.ubuntu.state === "IN_PROGRESS") {
							node$.append("<p>Ubuntu build in progress</p>");
						}
						if (data.android.state === "DONE") {
							node$.append("<p><a class='btn btn-lg' href='" + data.android.url + "'>Download Android APK</a></p>");
						}
						if (data.ubuntu.state === "DONE") {
							node$.append("<p><a class='btn btn-lg' href='" + data.ubuntu.url + "'>Download Android APK</a></p>");
						}

					}

				});

			}

			window.setInterval(function() {
				$("div[data-role='downloadProjects']").each(function() {
					retrieveAndAddBuildDataToDom($(this));
				});
			}, 1000);

		});