function createStubs(context) {

    var $ = window.top.jQuery;
    var barcode = null;

    function initScan() {

        $('#wr-barcode-emulator').remove();

        /* Creates fake 'back' button and hides the original one */
        $('#platform-events-fire-back').css("display", "none");
        $('#platform-events-fire-suspend').before("<button id=\"platform-events-fire-back-barcode\">Back</button>");
        $('#platform-events-fire-back-barcode').button().css("width", "90px");

        var scanBarcodeTemplate = [
                "<section id=\"wr-barcode-emulator\" class=\"overlay\" style=\"display:none; background: #fff; position: absolute; width: 100%; height: 100%; z-index: 10000;\">",
                "<div id=\"wr-barcode-title\" style=\"background: #000; font-size: 1em; color: #E6E6E6; font-weight: bold; line-height: 44px;padding: 0 10px;position: absolute;top: 0;left: 0;right: 0;\">",
                "<div style=\"display: inline-block; color: #E6E6E6; \">Scan barcode</div>",
                "<div id=\"cancel-barcode\" style=\" cursor: pointer;  float: right;\">CANCEL</div>", "</div>",
                "<div style=\"margin: 44px 0;\">",
                "<div style=\"padding: 10px;\"><div style=\"line-height: 1.5em;\">Format</div><div>",
                "<select id=\"wr-barcode-format\">", "<option value=\"QR_CODE\">QR_CODE</option>",
                "<option value=\"CODE_128\">CODE_128</option>", "<option value=\"CODE_39\">CODE_39</option>",
                "<option value=\"DATA_MATRIX\">DATA_MATRIX</option>", "<option value=\"EAN_8\">EAN_8</option>",
                "<option value=\"EAN_13\">EAN_13</option>", "<option value=\"ITF\">ITF</option>",
                "<option value=\"UPC_E\">UPC_E</option>", "<option value=\"UPC_A\">UPC_A</option>", "</select></div></div>",
                "<div style=\"padding: 10px;\"><div style=\"line-height: 1.5em;\">Value</div><div>",
                "<textarea id=\"wr-barcode-text\" rows=\"4\" style=\"width: 100%; box-sizing: border-box;\">",
                "</textarea></div></div>", "<button id=\"wr-scan-button\" style=\"margin: 10px;\">", "scan", "</button>", "</div>",
                "</section>" ].join("\n");

        var scanBarcode = $(scanBarcodeTemplate);
        $('#overlay-views').append(scanBarcode);
        return scanBarcode;
    }
    context.loadScriptFile("jquery.qrcode-0.11.0.js");
    context.loadScriptFile("jquery-barcode.js");
    return {
        BarcodeScanner: {
            encode: function(options) {
                var encodePromise = new Promise(function(resolve, reject) {
                    var data = {};
                    if (options["format"] === "QR_CODE") {
                        var container = $("<div></div>");
                        // $('body').append('<div id="qrContainer"></div>');
                        // $('#qrContainer').hide();
                        container.qrcode({
                            render: 'image',
                            minVersion: 1,
                            maxVersion: 40,
                            ecLevel: 'L',
                            left: 0,
                            top: 0,
                            size: 200,
                            fill: '#000',
                            background: null,
                            text: options["data"],
                            radius: 0,
                            quiet: 0,
                            mode: 0,
                            mSize: 0.1,
                            mPosX: 0.5,
                            mPosY: 0.5,
                            label: 'no label',
                            fontname: 'sans',
                            fontcolor: '#000',
                            image: null
                        });
                        var base64 = container.find('img').attr('src');
                        // $('#qrContainer').remove();
                        data["file"] = base64;
                    } else {
                        var settings = {
                            bgColor: "#FFFFFF",
                            color: "#000000",
                            barWidth: "1",
                            barHeight: "50",
                            addQuietZone: false
                        };
                        data["file"] = $('<div></div>').barcode(options["data"], "code128", settings) || null;
                    }
                    resolve(data);
                });
                return encodePromise;
            },
            scan: function() {
                barcode = initScan();
                var scanPromise = new Promise(function(resolve, reject) {

                    var cancel = function(e) {
                        var result = {
                            "cancelled": "true",
                            "text": "",
                            "format": ""
                        };

                        /* Restores original 'back' button */
                        $('#platform-events-fire-back-barcode').remove();
                        $('#platform-events-fire-back').css("display", "");

                        barcode.hide('slide', {
                            direction: 'down',
                            duration: 250
                        });

                        resolve(result);
                    };

                    /* User clicks 'back' button */
                    $('#platform-events-fire-back-barcode').click(cancel);

                    $('#cancel-barcode', barcode).click(cancel);

                    /* User clicks 'scan' button */
                    $('#wr-scan-button').button().click(function(e) {
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
                        $('#platform-events-fire-back').css("display", "");

                        barcode.hide('slide', {
                            direction: 'down',
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
                        direction: 'down',
                        duration: 250
                    });
                })
                return scanPromise;
            }
        }
    };
};