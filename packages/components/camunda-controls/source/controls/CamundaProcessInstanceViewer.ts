import {HTMLFormComponent} from 'fh-forms-handler';
import NavigatedViewer from "bpmn-js/lib/NavigatedViewer";
import {Shape} from 'diagram-js/lib/model/Types';
import Canvas from "diagram-js/lib/core/Canvas";
import type ElementRegistry from "diagram-js/lib/core/ElementRegistry";
import BpmnViewer from "bpmn-js/lib/NavigatedViewer";
import DrilldownModule from 'bpmn-js/lib/features/drilldown';

class CamundaProcessInstanceViewer extends HTMLFormComponent {
    protected processDefinitionBpmn: string;
    protected bpmnActivityIds: string[];
    protected incidentBpmnActivityIds: string[];
    protected selectedBpmnElementIdsChain: string[]; //activity lub incidentActivity
    protected height: string;
    protected viewer: BpmnViewer;
    protected canvas: any;

    protected navControlsData: NavControlsData = new NavControlsData();


    constructor(componentObj: any, parent: HTMLFormComponent) {

        super(componentObj, parent);
        this.processDefinitionBpmn = this.componentObj.processDefinitionBpmn;
        this.bpmnActivityIds = this.componentObj.bpmnActivityIds;
        this.selectedBpmnElementIdsChain = this.componentObj.selectedBpmnElementIdsChain;
        this.incidentBpmnActivityIds = this.componentObj.incidentBpmnActivityIds;
        this.height = this.componentObj.height;

        this.navControlsData.navigationControlsVisible = this.componentObj.navigationControlsVisible?.toLowerCase() !== 'false';
        this.navControlsData.zoomInBtnHint = this.componentObj.zoomInBtnHint;
        this.navControlsData.zoomOutBtnHint = this.componentObj.zoomOutBtnHint;
        this.navControlsData.zoomResetBtnHint = this.componentObj.zoomResetBtnHint;
    }

    create() {
        let container = document.createElement('div');
        container.id = this.id;
        this.component = container;

        this.wrap(true);
        this.handlemarginAndPAddingStyles();
        this.display();

        container.style.userSelect = 'none'; //user nie zaznacza tekstów

        this.viewer = new NavigatedViewer({
            container: container,
            height: this.height || '100px', //uwaga z wysokością - potrzebny jest default, bez którego powstanie wyjątek
            additionalModules: [
                DrilldownModule
            ]
        });
        this.displayDiagram();

    };

    update(change) {
        $.each(change.changedAttributes, function (name, newValue) {
            switch (name) {
                case 'processDefinitionBpmn':
                    this.processDefinitionBpmn = newValue;
                    break;
                case 'bpmnActivityIds':
                    this.bpmnActivityIds = newValue;
                    break;
                case 'selectedBpmnElementIdsChain':
                    this.selectedBpmnElementIdsChain = newValue;
                    break;
                case 'incidentBpmnActivityIds':
                    this.incidentBpmnActivityIds = newValue;
                    break;
            }
            this.displayDiagram();
        }.bind(this));
    };


    displayDiagram() {
        if (this.processDefinitionBpmn != null) {

            this.viewer.importXML(this.processDefinitionBpmn).then(r => {
                    drawWhenReady(this.viewer, this.bpmnActivityIds, this.incidentBpmnActivityIds, this.selectedBpmnElementIdsChain, this.htmlElement, this.navControlsData)
                }
            )
        }

        //importXml zachodzi szybciej niż tworzenie wyświetlanie okna przez fh;
        //metoda próbuje operować synchronicznie a gdy się nie uda to MAX_RETRIES, CO RETRY_DELAY; obserwowaony sukces to  2 - 3 retry
        function drawWhenReady(viewer: BpmnViewer, bpmnActivityIds: string[], incidentBpmnActivityIds: string[], selectedBpmnElementIdsChain: string[],
                               htmlElement: HTMLElement, navControlsData) {
            const MAX_RETRIES = 10;
            const RETRY_DELAY_MS = 200;
            let attempt = 0;

            const draw = () => {
                const diagramCanvas = viewer.get<Canvas>('canvas');
                if (diagramCanvas.viewbox().width === 0 || diagramCanvas.viewbox.length === 0) { //gdy viebox początkowo na zero, operacje typu zoom nie powiodą się
                    diagramCanvas.resized() //usuwa precomputed viebox, zoom przeliczy na nowo
                    if (attempt >= MAX_RETRIES) {
                        console.error(`bpmn viewer: container has no size after ${MAX_RETRIES} tries`, htmlElement);
                        return;
                    }
                    attempt++;
                    setTimeout(draw, RETRY_DELAY_MS);
                    return;
                }

                clearBadges(viewer, 'token');
                clearBadges(viewer, 'incident');

                bpmnActivityIds?.forEach(a => addBadge(viewer, {elementId: a, type: 'token',skipExpandedContainer:false}));
                incidentBpmnActivityIds?.forEach(a => addBadge(viewer, {elementId: a, type: 'incident', skipExpandedContainer:true}));

                if (selectedBpmnElementIdsChain) {
                    navigateToActivity(viewer, selectedBpmnElementIdsChain, 1.2);
                } else {
                    diagramCanvas.zoom('fit-viewport');
                }
                if(navControlsData.navigationControlsVisible)
                    addNavigationControls(diagramCanvas.getContainer(), viewer, navControlsData)

            };
            draw();
        }

        function addNavigationControls(
            container: HTMLElement,
            viewer: NavigatedViewer,
            navControlsData: NavControlsData
        ): void {


            const canvas = viewer.get<Canvas>('canvas');
            container.style.position = 'relative';

            const panel = document.createElement('div');
            panel.className = 'bpmn-nav-panel';


            const resetBtn = createButton(navControlsData.zoomResetBtnHint ?? "", `<i class="fas fa-compress" style="pointer-events:none"></i>`);
            resetBtn.addEventListener('click', () => canvas.zoom('fit-viewport'));

            const zoomGroup = document.createElement('div');
            zoomGroup.className = 'bpmn-zoom-group';

            const zoomInBtn = createButton(navControlsData.zoomInBtnHint ?? "", `<i class="fas fa-plus"  style="pointer-events:none"></i>`);
            const zoomOutBtn = createButton(navControlsData.zoomOutBtnHint ?? "", `<i class="fas fa-minus" style="pointer-events:none"></i>`);

            zoomInBtn.addEventListener('click', () => canvas.zoom((canvas.zoom() as number) * 1.2));
            zoomOutBtn.addEventListener('click', () => canvas.zoom((canvas.zoom() as number) / 1.2));

            zoomGroup.appendChild(zoomInBtn);
            zoomGroup.appendChild(zoomOutBtn);

            panel.appendChild(resetBtn);
            panel.appendChild(zoomGroup);
            container.appendChild(panel);
        }

        function createButton(tooltipContent: string, iconHtml: string): HTMLButtonElement {
            const btn = document.createElement('button');
            btn.className = 'bpmn-nav-btn';
            btn.innerHTML = iconHtml;
            btn.setAttribute('data-placement', 'right');
            if(!""){
                $(btn).tooltip({
                    placement: 'right',
                    title: tooltipContent
                });
            }

            return btn;
        }


        const BADGE_POSITION: Record<'token' | 'incident', object> = {
            token: { bottom: -3, left: -10 },
            incident: { bottom: -3, left: 10 }
        };

        function addBadge(viewer: NavigatedViewer, {elementId, type, skipExpandedContainer}: BadgeOptions): void {
            const overlays = viewer.get('overlays') as {
                add: (elementId: string, type: string, descriptor: object) => string;
                get: (filter: object) => Array<{ html: HTMLElement }>;
            };
            const elementRegistry = viewer.get('elementRegistry') as {
                get: (id: string) => { type?: string; collapsed?: boolean } | undefined;
            };

            const element = elementRegistry.get(elementId);
            if (!element) return;

            if (skipExpandedContainer && element.type === 'bpmn:SubProcess' && element.collapsed === false) {
                return;
            }

            const existing = overlays.get({element: elementId, type});

            if (existing.length > 0) {
                const badgeEl = existing[0].html;
                const current = parseInt(badgeEl.textContent ?? '0', 10);
                badgeEl.textContent = String(current + 1);
                return;
            }

            const badgeEl = document.createElement('div');
            badgeEl.className = `bpmn-badge bpmn-badge--${type}`;
            badgeEl.textContent = '1';

            overlays.add(elementId, type, {
                position: BADGE_POSITION[type],
                html: badgeEl,
            });
        }

        function clearBadges(viewer: NavigatedViewer, type?: 'token' | 'incident'): void {
            const overlays = viewer.get('overlays') as {
                remove: (filter: object) => void;
            };
            overlays.remove(type ? {type} : {});
        }

        function navigateToActivity(viewer: BpmnViewer, selectedBpmnElementIdsChain: string[], zoomLevel = 1.0): void {
            const canvas = viewer.get<Canvas>('canvas');

            const path = selectedBpmnElementIdsChain;
            if (!path || path.length === 0) return;

            const drillPath = path.slice(0, -1);
            const target = path[path.length - 1];

            const mainRoot = canvas.getRootElements()[0];
            if (mainRoot && canvas.getRootElement() !== mainRoot) {
                canvas.setRootElement(mainRoot);
            }

            for (const step of drillPath) {
                const planeRoot = canvas.getRootElements().find(r => r.businessObject?.id === step);
                if (planeRoot) {
                    if (canvas.getRootElement() !== planeRoot) {
                        canvas.setRootElement(planeRoot);
                    }
                }
                // brak planeRoot = expanded subprocess inline na bieżącej planie -> nic nie robimy
            }

            canvas.resized();
            zoomToElement(viewer, target, zoomLevel);
        }

        function zoomToElement(viewer: BpmnViewer, elementId: string, zoomLevel = 1.0): void {
            const canvas = viewer.get<Canvas>('canvas');
            const elementRegistry = viewer.get<ElementRegistry>('elementRegistry');

            const element = elementRegistry.get(elementId) as Shape;
            if (!element) {
                throw new Error(`Element "${elementId}" not found`);
            }

            const viewportWidth = canvas.viewbox().outer.width;
            const viewportHeight = canvas.viewbox().outer.height;

            // środek elementu trafia na środek viewportu
            canvas.viewbox({
                x: element.x + element.width / 2 - (viewportWidth / 2) / zoomLevel,
                y: element.y + element.height / 2 - (viewportHeight / 2) / zoomLevel,
                width: viewportWidth / zoomLevel,
                height: viewportHeight / zoomLevel,
            });
        }

    }
}

class NavControlsData {
    zoomInBtnHint: string;
    zoomOutBtnHint: string;
    zoomResetBtnHint: string;
    navigationControlsVisible: boolean;
}


interface BadgeOptions {
    elementId: string;
    type: 'token' | 'incident';
    skipExpandedContainer: boolean;
}

export {CamundaProcessInstanceViewer};
