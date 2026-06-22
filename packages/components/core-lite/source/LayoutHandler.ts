import {injectable} from "inversify";

declare const ENV_IS_DEVELOPMENT: boolean;
declare const $ :any;

const LAYOUT_GLOBAL_ELEMENTS = [
    'mainForm',
    'menuForm',
    'navbarForm',
];

@injectable()
class LayoutHandler {

    public static mainLayout:string = "standard";
    private prefix:string = "fh-layout-";

    public currentMainLayout:string = LayoutHandler.mainLayout;
    public targetLayout:string = LayoutHandler.mainLayout;

    /**
     * Functions that finds specific container in target layout
     * Created for bakward compatibility with JS function getElementById.
     * @param containerId
     * @param jqueryObject
     * @return HTML DOM Object or null*
     */
    public getLayoutContainer(containerId:string, jqueryObject:boolean = false){
        const container = $("#"+this.targetLayout+" #"+containerId);
        if(container.length == 0){
            const containerFallBack = $("#"+containerId);
            if(containerFallBack.length == 0){
                return null
            } else {
                return (jqueryObject ? containerFallBack : containerFallBack[0]);
            }
        } else {
           return (jqueryObject ? container : container[0]);
        }

    }

    /**
     * Functions that finds specific container in current layout
     * Created for bakward compatibility with JS function getElementById.
     * @param containerId
     * @param jqueryObject
     * @return HTML DOM Object or null*
     *
     */
    public getCurrentLayoutContainer(containerId:string, jqueryObject:boolean = false){
        const container = $("#"+this.currentMainLayout+" #"+containerId);
        if(container.length == 0){
            const containerFallBack = $("#"+containerId);
            if(containerFallBack.length == 0){
                return null
            } else {
                return (jqueryObject ? containerFallBack : containerFallBack[0]);
            }
        } else {
            return (jqueryObject ? container : container[0]);
        }

    }

    public getCurrentMainLayout(){
        return this.currentMainLayout;
    }

    /**
     * Function that prepare age for layout processing. If layout will be changed function hides all layouts.
     * @param layout
     */
    public startLayoutProcessing(layout:string):void {
        if(this.currentMainLayout != layout) {
            $(".fh-layout-div").addClass("d-none");
            this.targetLayout = this.prefix+layout;
        }
        if (ENV_IS_DEVELOPMENT) {
            console.log("startLayoutProcessing", layout, this.currentMainLayout, this.targetLayout);
        }
    }

    /**
     * Fuction that block layout change. Used when another UC shows form on modal element.
     * Used in Form.ts;
     * @param isModal
     */
    public blockLayoutChangeForModal() :void{

        this.targetLayout = this.currentMainLayout;

        if (ENV_IS_DEVELOPMENT) {
            console.log("blockLayoutChangeForModal", this.currentMainLayout, this.targetLayout);
        }
    }

    /**
     * Fuction that block layout change. Used when UC is in design mode
     * Used in Form.ts;
     */
    public blockLayoutChangeForDesigner(){

        this.targetLayout = LayoutHandler.mainLayout;

        if (ENV_IS_DEVELOPMENT) {
            console.log("blockLayoutChangeForDesigner", this.currentMainLayout, this.targetLayout);
        }

    }

    /**
     * Function that finish layout processing. Moves exist contetnt from one layout to another.
     * Moving designer components is not implemented.
     */
    public finishLayoutProcessing(){
        // Uwzględniamy specjalne traktowanie layout "standard", które w zmiennych jest pozbawione prefix
        const targetLayoutElement = this.targetLayout === 'standard'
            ? document.getElementById(this.prefix + this.targetLayout)
            : document.getElementById(this.targetLayout);

        if(this.currentMainLayout != this.targetLayout) {
            if (ENV_IS_DEVELOPMENT) {
                console.log("Zmiana aktywnego layout");
            }
            // Uwzględniamy specjalne traktowanie layout "standard", które w zmiennych jest pozbawione prefix
            const currentLayoutElement = this.currentMainLayout === "standard"
                ? document.getElementById(this.prefix + this.currentMainLayout)
                : document.getElementById(this.currentMainLayout);
            // dajemy sygnał zmiany aktywnego layout, aby ewentualni obserwatorzy mogli wywołać akcje typu destroy
            currentLayoutElement.classList.remove("active-layout");
            // domyślna lista z identyfikatorami elementów na stronie, które mają zostać skopiowane
            const copyElements = new Set<string>(LAYOUT_GLOBAL_ELEMENTS);
            // opcjonalna lista z identyfikatorami elementów na stronie, które mają zostać skopiowane
            const targetLayoutWrapper: HTMLElement | null = targetLayoutElement.querySelector<HTMLElement>('[data-custom-elements-list]');
            const customElementsList: string | undefined = targetLayoutWrapper?.dataset.customElementsList;
            if (customElementsList && customElementsList.length > 0) {
                customElementsList.split(',')
                    .filter(elementId => elementId && elementId.trim().length > 0)
                    .forEach(element => copyElements.add(element.trim()));
            }
            copyElements.forEach(elementId => {
                const currentElement= this.getCurrentLayoutContainer(elementId, true);
                const targetElement = this.getLayoutContainer(elementId, true);
                currentElement.contents().appendTo(targetElement);
                currentElement.html("");
            })
            // Ustawiamy nowy layout jako pierwszy w drzewie DOM, aby naprawić błąd z obsługą akcji w menu.
            currentLayoutElement.before(targetLayoutElement);
            // dajemy sygnał zmiany aktywnego layout, aby ewentualni obserwatorzy mogli wywołać akcje typu init
            targetLayoutElement.classList.add("active-layout");
            this.currentMainLayout = this.targetLayout;

        targetLayoutElement.classList.remove("d-none");

        if (ENV_IS_DEVELOPMENT) {
            console.log("finishLayoutProcessing", this.currentMainLayout, this.targetLayout);
        }
    }

}

export {LayoutHandler};