## Control Combo

| <center>Identifier </center>  |  <center>Type  </center> | <center> Boundable  </center> |  <center>DefaultValue </center>  |  <center>Description  </center>    |  
|-----------|------|-----------|--------------|----------------| 
|availability|AccessibilityEnum|true||Accessibility of an Component|
|confirmOnEvent|String|false||Defines pipe-separated list of events name that require confirmation dialog. Eg. onClick or onInput , onChange|
|confirmationMsg|ModelBinding|true||Binding represents value from confirmation message, used inside of '{}', like {model}.|
|cursor|ModelBinding|true||Binding represents cursor from model of Form, used inside of '{}', like {model}.|
|displayExpression|String|true||Rule which will be used to format items as text.|
|displayFunction|ModelBinding|true||Name of model object (java.util.function.Function) which will be used to format items as text.|
|emptyValue|ModelBinding|true|false|Defines if value passed can be empty|
|filterFunction|ModelBinding|true|Default function: (model, value) -> ((String) model).toLowerCase().contains(value.toLowerCase())|Name of model object (java.util.function.BiPredicate) which will be used to filter items by text.|
|filterText|ModelBinding|true||Binding represents value of filter text|
|formatter|String|false||Id of formatter which will format object to String. It must be consistent with value of pl.fhframework.formatter.FhFormatter annotation.|
|freeTyping|ModelBinding|true|false|Defines if new values could be typed be user. Binding changes may not be respected after initially showing this control.|
|height|String|false||Component height in "px" or "%", "px" is default height unit. There should not be any character between number and unit - height="80px" is valid, height="80 px" is invalid.|
|hiddenElementsTakeUpSpace|boolean|false|false|Parameter for HIDDEN components. Makes hidden elements still take up space in the page.|
|hint|ModelBinding|true||Hint for component, visible after hovering over specified component part|
|hintPlacement|HintPlacement|false|TOP|Placement of the hint for component. Available values: top, left, right, bottom. If value is not set then position will be chosen dynamically.|
|hintTrigger|HintTrigger|false|HOVER_FOCUS|Trigger of the hint for component. Available values: HOVER_FOCUS, HOVER. If value is not set then position will be HOVER_FOCUS.|
|horizontalAlign|HorizontalAlign|false|left|Component bootstrap option to place component on the left, center or right side of the view.|
|icon|ModelBinding|true||Icon id. Please refer to http://fontawesome.io/icons/ for all available icons.|
|iconAlignment|IconAlignment|true|before|Icon alignment - possible values are before or after. Final alignment depends of component where this attribute is used.|
|id|String|false||Component identifier (should be unique within the view)|
|inputSize|double|false|60.0|Proportional size of input, inputSize should be set when labelPosition is "left" or "right".|
|keyEvent|String|false||Defines pipe-separated list of key definitions that will call 'onKeyEvent' action. Eg. ENTER or ENTER|CTRL+ALT+A|CTRL+B|SPACE|
|label|ModelBinding|true||Represents label for created component. Supports FHML - FH Markup Language.|
|labelId|String|false||Defines label control id.|
|labelPosition|LabelPosition|false||Defines position of a label. Position is one of: up, down, left, right.|
|labelSize|String|false||Proportional size of label, labelSize should be set when labelPosition is "left" or "right". If this value is set, then inputSize property does not work.|
|marginBottom|String|false||Component margin amount in "px" to leave outter gap at the bottom. There should not be any character between number and unit - marginBottom="10px" is valid, marginBottom="10 px" is invalid.|
|marginLeft|String|false||Component margin amount in "px" to leave outter gap on the left side. There should not be any character between number and unit - marginLeft="10px" is valid, marginLeft="10 px" is invalid.|
|marginRight|String|false||Component margin amount in "px" to leave outter gap on the right side. There should not be any character between number and unit - marginRight="10px" is valid, marginRight="10 px" is invalid.|
|marginTop|String|false||Component margin amount in "px" to leave outter gap at the top. There should not be any character between number and unit - marginTop="10px" is valid, marginTop="10 px" is invalid.|
|multiselect|boolean|false|false|Determines if multiselect is enabled in combo. If multiselect is set to true, value has to be set to Collection.|
|onChange|ActionBinding|false||If there is some value, representing method in use case, then on every action in input, that method will be executed. This method fires, when component loses focus.|
|onDblSpecialKey|ActionBinding|false||If there is some value, representing method in use case, that will be called every time a special key (Ctrl+Space) is pressed 2 times.|
|onDesignerToolboxDrop|ActionBinding|false||If the component is dropped on form edited in designer.|
|onEmptyValue|ActionBinding|false||If there is some value, representing method in use case, then on clearing value, that method will be executed. Action is fired, while component is active.|
|onInput|ActionBinding|false|-|If there is some value, representing method in use case, then on every action in input, that method will be executed. Action is fired, while component is active.|
|onInputTimeout|Integer|false|0|Delay onInput action for specific miliseconds. Value must be between 0 and 10000.|
|onKeyEvent|ActionBinding|false||Represents use case's action executed each time key(s) defined in 'keyEvent' are being pressed.|
|onSpecialKey|ActionBinding|false||If there is some value, representing method in use case, that will be called every time a special key (Ctrl+Space) is pressed.|
|openOnFocus|Boolean|false|true|Should prompt window be opened when field gains focus.|
|paddingBottom|String|false||Component padding amount in "px" to leave outter gap at the bottom. There should not be any character between number and unit - paddingBottom="10px" is valid, paddingBottom="10 px" is invalid. Only positive values.|
|paddingLeft|String|false||Component padding amount in "px" to leave outter gap at the bottom. There should not be any character between number and unit - paddingLeft="10px" is valid, paddingLeft="10 px" is invalid. Only positive values.|
|paddingRight|String|false||Component padding amount in "px" to leave outter gap at the bottom. There should not be any character between number and unit - paddingRight="10px" is valid, paddingRight="10 px" is invalid. Only positive values.|
|paddingTop|String|false||Component padding amount in "px" to leave outter gap at the bottom. There should not be any character between number and unit - paddingTop="10px" is valid, paddingTop="10 px" is invalid. Only positive values.|
|preload|boolean|false|false|Defines if combo values should be present even if no text is typed|
|required|ModelBinding|true|false|User can define if component is required for Form. Binding changes may not be respected after initially showing this control.|
|styleClasses|String|false||Component style classes, should be separated by ',' character|
|validationLabel|ModelBinding|true||Represents label for created component used in validation messages. If not set, falls back to label attribute's value.|
|validationRule|String|false||User can define validation rule for binded model using SpEL. Expression is based on properties of form's model and must be prefixed with '-' sign, eg. -prop1 < prop2|
|value|ModelBinding|true||Binding represents value from model of Form, used inside of '{}', like {model}.|
|verticalAlign|VerticalAlign|false|bottom|Option to align component vertically relative to parent element. Available values: top, middle, bottom.|
|width|String|false|"md-12" for all components but not Button FileUpload or FileDownload - "md-2"|Component bootstrap size option to place component in one row and different column width. For Column this value should be numeric, because it will be translated to percentage value.|
