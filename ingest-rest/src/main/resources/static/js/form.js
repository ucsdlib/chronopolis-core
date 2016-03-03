/**
 * slide toggle our filter forms
 *
 * Created by shake on 1/12/16.
 */

$(document).ready(function () {
    $(function () {
        $("#dropdown").on('click', function () {
            $("#filter-body").slideToggle("slow", function () {
            });
        });
    });
});

