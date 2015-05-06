$(document).ready(
		function() {

			$("#example p:first").css("display", "block");

			jQuery.fn.timer = function() {
				if (!$(this).children("p:last-child").is(":visible")) {
					$(this).children("p:visible").css("display", "none").next(
							"p").css("display", "block");
				} else {
					$(this).children("p:visible").css("display", "none").end()
							.children("p:first").css("display", "block");
				}
			};

			window.setInterval(function() {
				$("#example").timer();
			}, 1000);

		});