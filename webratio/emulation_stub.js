function createStubs() {

    var $ = window.top.jQuery;
    var barcode = null;

    function initScan() {

        $('#wr-barcode-emulator').remove();

        /* Creates fake 'back' button and hides the original one */
        $('#platform-events-fire-back').css("display", "none");
        $('#platform-events-fire-suspend')
                .before(
                        "<button id=\"platform-events-fire-back-barcode\" class=\"ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only\"><span class=\"ui-button-text\">Back</span></button>");
        $('#platform-events-fire-back-barcode').css("width", "90px");

        var scanBarcodeTemplate = [
                "<section id=\"wr-barcode-emulator\" style=\"display:none; background: rgba(0, 0, 0, 00); position: absolute; width: 100%; height: 100%; z-index: 10000;\">",
                "<div style=\"background: #fff; height: 100%; width: 100%; overflow: auto; \">", "<select id=\"wr-barcode-format\">",
                "<option value=\"QR_CODE\">QR_CODE</option>", "<option value=\"CODE_128\">CODE_128</option>",
                "<option value=\"CODE_39\">CODE_39</option>", "<option value=\"DATA_MATRIX\">DATA_MATRIX</option>",
                "<option value=\"EAN_8\">EAN_8</option>", "<option value=\"EAN_13\">EAN_13</option>",
                "<option value=\"ITF\">ITF</option>", "<option value=\"UPC_E\">UPC_E</option>",
                "<option value=\"UPC_A\">UPC_A</option>", "</select>", "<textarea id=\"wr-barcode-text\" rows=\"4\" cols=\"50\">",
                "</textarea>", "<button id=\"wr-scan-button\">", "scan", "</button>", "</div>", "</section>" ].join("\n");

        var scanBarcode = $(scanBarcodeTemplate);
        $('#overlay-views').append(scanBarcode);
        return scanBarcode;
    }

    return {
        BarcodeScanner: {
            encode: function() {

                var encodePromise = new Promise(function(resolve, reject) {

                })
                return encodePromise;
            },
            scan: function() {
                barcode = initScan();
                var scanPromise = new Promise(function(resolve, reject) {

                    /* User clicks 'back' button */
                    $('#platform-events-fire-back-barcode').click(function(e) {
                        var result = {
                            "cancelled": "true",
                            "text": "",
                            "format": ""
                        };

                        /* Restores original 'back' button */
                        $('#platform-events-fire-back-barcode').remove();
                        $('#platform-events-fire-back').css("display", "initial");

                        barcode.hide('slide', {
                            direction: 'left',
                            duration: 250
                        });

                        resolve(result);
                    });

                    /* User clicks 'scan' button */
                    $('#wr-scan-button').click(function(e) {
                        var format = $('#wr-barcode-format').val();
                        var text = $('#wr-barcode-text').val();

                        /* Checks if textarea is empty */
                        if (text === "") {
                            $('#wr-barcode-text').val("Enter a text here.");
                            return;
                        }

                        var result = {};
                        result.cancelled = false;
                        result.format = format;
                        result.text = text;

                        /* Restores original 'back' button */
                        $('#platform-events-fire-back-barcode').remove();
                        $('#platform-events-fire-back').css("display", "initial");

                        barcode.hide('slide', {
                            direction: 'left',
                            duration: 250
                        });

                        resolve(result);
                    });

                    /* Empty textarea */
                    $('#wr-barcode-text').click(function(e) {
                        if ($('#wr-barcode-text').val() === "Enter a text here.") {
                            $('#wr-barcode-text').val('');
                        }
                    });

                    barcode.show('slide', {
                        direction: 'right',
                        duration: 250
                    });
                })
                return scanPromise;
            }
        }
    };
};