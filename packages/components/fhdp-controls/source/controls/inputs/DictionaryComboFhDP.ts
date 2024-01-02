import {createElement} from 'react';
import * as ReactDOM from 'react-dom';
import {HTMLFormComponent, LanguageChangeObserver} from 'fh-forms-handler';
import {LanguageResiterer} from '../helpers/LanguageResigerer';
import { ComboFhDP } from './ComboFhDP';
import { DictionaryComboFhDPHelper } from '../helpers/DictionaryComboFhDPHelper';
import { DictionaryComboFhDPPopperTable } from './DictionaryComboFhDPPopperTable';

export type PaginationPlacement = "TOP" | "BOTTOM";

export type RenderPopupProps = {
    hookElementId: string;
    parent: any;
    position: 'left' | 'right';
}

class DictionaryComboFhDP extends ComboFhDP implements LanguageChangeObserver {
    private instance : any;
    private divTooltipId: any;
    private divTooltip: any;

    private title: string;
    private columns: any[];
    private rows: any[];
    private popupOpen: boolean;
    private searchRequested: boolean;
    private page: number;
    private pagesCount: number;
    private isSearch: boolean = true;
    private popupColor?: string;
    private dirty: boolean = false;
    private languageWrapped: any;
    private valueFromChangedBinding: any;
    private displayOnlyCode: boolean;

    private _writingDebaunceTimer: any;
    private pageChangeClicked: boolean = false;
    private clickInPopup: boolean = false;

    public guuid: string;
    private paginationPlacement: PaginationPlacement;

    protected lastValueHtmlElement: any;
    protected lastValueGroupSpanElement: any;
    protected lastValueGroupSearchElement: any;
    private isLastValueTooltip: boolean = false;
    private codeValue: string;

    constructor(componentObj: any, parent: HTMLFormComponent) {
        super(componentObj, parent);
        this.codeValue = componentObj.codeValue;
        this.instance = null;
        this.divTooltipId = null;
        this.divTooltip = null;
        this.title = this.fhml.resolveValueTextOrEmpty(this.componentObj.title);
        this.columns = this.componentObj.columns || [];
        this.rows = this.componentObj.rows || [];
        this.popupOpen = false;
        this.page = this.componentObj.page;
        this.searchRequested = this.componentObj.searchRequested;
        this.pagesCount = this.componentObj.pagesCount;
        this.displayOnlyCode = this.componentObj.displayOnlyCode;
        this.paginationPlacement = this.componentObj.paginationPlacement;
        // this.dirty = this.componentObj.dirty;
        // console.log('dirty on create', this.componentObj.dirty)
        this.dirty = false;
        this.languageWrapped = this.componentObj.language || null;
        if (this.componentObj.valueFromChangedBinding) {
            this.valueFromChangedBinding = this.componentObj.valueFromChangedBinding;
        }
        if (this.componentObj.background) {
            this.popupColor = this.componentObj.popupColor;
        }

        LanguageResiterer.getInstance(this.i18n).registerLanguags(this);

    }

    create() {
        super.create();
        let self = this;

        let inputGroup = this.getInputGroupElement();
        let iconSearch = this.getIconClass();

        self.divTooltipId = Date.now();
        self.divTooltip = document.createElement('div');
        self.divTooltip.classList.add('dc-element');

        self.divTooltip.classList.add('hidden-popper');
        self.divTooltip.id = self.divTooltipId.toString();

        const id = `FhDP-dictionary-combo-${+new Date()}`;
        this.getInputGroupElement().id = id
        this.guuid = id;

        this.getInputGroupElement().appendChild(self.divTooltip);

        this.i18n.subscribe(this);

        DictionaryComboFhDPHelper.getInstance().registerElement(this);

        this.display();
        document.getElementById(this.input.id).addEventListener('change', this.handleRawDataChange.bind(this));
//         document.getElementById(this.input.id).addEventListener('keypress', this.handleTextInputChange.bind(this));
        this.input.addEventListener('blur', this.handleTextInputBlur.bind(this));
        document.getElementById(this.input.id).addEventListener('keydown', this.handleTextInputChange.bind(this));

        if(iconSearch != null){
            $($("div.search-icon", inputGroup)[0]).addClass('fc-editable');
            $($("div.search-icon", inputGroup)[0]).on("click", function(){
                if(self.accessibility === "VIEW" && !!self.codeValue){//w trybie VIEW nie działa onClickSearchIcon
                    self.isSearch = true;
                    self.crateTooltip($("div.search-icon", self.getInputGroupElement())[0]);
                }
            })
        }

        if(this.lastValueHtmlElement != null) {
            $($("div.search-icon", this.lastValueHtmlElement)[0]).addClass('fc-editable');
            $($("div.search-icon", this.lastValueHtmlElement)[0]).on("click", function(){
                if(self.accessibility === "VIEW"){
                    self.isSearch = true;
                    self.crateTooltip($("div.search-icon", self.lastValueHtmlElement)[0], true);
                }
            })
        }

        this.input.addEventListener('keypress', (ev) => {
            if (ev.key === "Enter") {
                this.changesQueue.queueAttributeChange('searchRequested', true);
                if (!this.popupOpen) {
                    this.popupOpen = true;
                }
                this.isSearch = true;
                this.fireEvent('onClickSearchIcon', 'search');
            }

            if (ev.key === "Escape") {
                this.changesQueue.queueAttributeChange('searchRequested', true);
                this.popupOpen = false;
                this.isSearch = true;
                this.fireEvent('onClickSearchIcon', 'search');
                this.renderPopup();
            }
        })
        let clearBtn = $("div.clearButton", self.getInputGroupElement())[0];
        if (this.accessibility !== 'VIEW') {
            if (clearBtn) {
                $(clearBtn).addClass('fc-editable');
                clearBtn.addEventListener('click', (ev) => {
                    this.input.value = "";
                    this.rawValue = null;
                    this.changesQueue.queueAttributeChange('text', null);
                    this.changesQueue.queueAttributeChange('cleared', true);
                    this.fireEventWithLock('cleanupSearch', 'cleanupSearch');
                    // this.fireEventWithLock('recordSelected', null);
                    if(this.popupOpen){
                        this.onClickSearchIconEvent(ev);
                    }
                    this.input.focus();
                });
            }
        } else {
            if (clearBtn) {
                clearBtn.style.display = 'none';
            }
        }

        if(!!this.lastValueHtmlElement) {
            this.lastValueHtmlElement.querySelector("div.search-icon").addEventListener('click', this.onClickEvent.bind(this));
        } else if(this.htmlElement.querySelector("span.input-old-value") != null) {
            this.htmlElement.querySelector("span.input-old-value").classList.add('fc-editable');
            this.htmlElement.querySelector("span.input-old-value").addEventListener('click', this.onClickEvent.bind(this));
        }

        if(this.htmlElement.querySelector("div.search-icon") != null) {
            this.htmlElement.querySelector("div.search-icon").addEventListener('click', this.onClickSearchIconEvent.bind(this));
        }

        if (this.rawValue === undefined || this.rawValue === 'null' || this.rawValue === null || this.rawValue === '') {
            this.input.value = '';

            if(!!this.isTableMode) {
                this.inputGroupElement.classList.add('hide-old-value');
            }
        }
        if(this.accessibility == 'EDIT') {
            this.fireEvent('setGuuid', JSON.stringify({id: this.guuid}));
        }
    }

    protected createLastValueElement() {
        if(!this.isTableMode) {
            super.createLastValueElement();
            return;
        }

        let lastValueText;
        if(this.componentObj.lastValue) {
            if (this.lastValueParser) {
                lastValueText = this.lastValueParser(this.lastValue);
            } else {
                lastValueText = this.lastValue;
            }
        } else if(this.componentObj.newValueText && this.componentObj.lastValue===undefined) {
            lastValueText = this.newValueText;
        }

        if(lastValueText) {
            let group = document.createElement('div');
            group.id = `FhDP-dictionary-combo-${+new Date()}-lastValue`;
            group.classList.add('input-group-append');

            let groupSpan = document.createElement('span');
            ['fc', 'form-control', 'fc-disabled', 'disabled', 'input-old-value'].forEach(function (cssClass) {
                groupSpan.classList.add(cssClass);
            });

            if(this.hideCrossed == "true"){
                groupSpan.classList.add('input-old-value-remove-line');
            }
            if(this.componentObj.lastValue && this.rawValue == this.lastValue){
                group.classList.add('hide-old-value');
            }

            groupSpan.innerText = lastValueText;
            group.appendChild(groupSpan);
            this.lastValueGroupSpanElement = groupSpan;

            let groupSearch = document.createElement('div');
            groupSearch.classList.add('input-group-prepend');
            groupSearch.classList.add('search-icon');
            groupSearch.addEventListener('click', this.onClickEvent.bind(this));

            let groupSpanSearch = document.createElement('span');
            groupSpanSearch.classList.add('input-group-text');
            groupSearch.appendChild(groupSpanSearch);

            this.htmlElement.classList.add('hasInputIcon');
            let icon = document.createElement('i');
            let classes = this.componentObj.icon.split(' ');
            icon.classList.add(classes[0]);
            if (classes[1]) {
                icon.classList.add(classes[1]);
            }
            groupSpanSearch.appendChild(icon);

            if (this.componentObj.iconAlignment === 'BEFORE') {
                group.insertBefore(groupSearch, groupSpan);
            } else if (this.componentObj.iconAlignment === 'AFTER') {
                groupSearch.classList.remove('input-group-prepend');
                groupSearch.classList.add('input-group-append');
                group.appendChild(groupSearch);
            } else {
                group.insertBefore(groupSearch, groupSpan);
            }

            if(!this.componentObj.lastValue) {
                this.lastValueGroupSpanElement.classList.remove('input-old-value');
                groupSearch.classList.add('d-none');
            }

            this.lastValueHtmlElement = group;
            this.lastValueGroupSearchElement = groupSearch;
            this.inputGroupElement.parentElement.appendChild(group);
        }
    }

    protected toogleLastValueElement(theSameValue) {
        if (!this.isTableMode) {
            super.toogleLastValueElement(theSameValue);
            return;
        }

        if(this.lastValueHtmlElement) {
            if(theSameValue){
                this.lastValueHtmlElement.classList.add('hide-old-value');
            } else {
                this.lastValueHtmlElement.classList.remove('hide-old-value');
            }
        }
    }

    validated(result: boolean) {
        if (result) {
            this.unmarkDirty();
            this.clickInPopup = false;
            this.popupOpen = false;
            this.renderPopup();

            this.fireEventWithLock('recordSelected', this.rawValue);
            if(window['handlePopupClose']) {
                window['handlePopupClose'](true);
            }
            this.fireEventWithLock('onChange', this.onChange);
        } else {
            if ((this.clickInPopup && this.popupOpen) || (this.pageChangeClicked && this.popupOpen)) {
                this.clickInPopup = false;
                this.pageChangeClicked = false;
            } else {
                this.changesQueue.queueAttributeChange('searchRequested', true);
                if (!this.popupOpen) {
                    this.popupOpen = true;
                }
                this.isSearch = true;
                this.fireEvent('onClickSearchIcon', 'search');
                this.crateTooltip($("div.search-icon", this.getInputGroupElement())[0]);
            }
            this.getInputGroupElement().style.border = 'solid red 1px';
            this.defferFunction(() => {
                document.getElementById(this.input.id).focus();
            });
        }
    }

    markDirty() {
        this.dirty = true;
        this.changesQueue.queueAttributeChange('dirty', this.dirty);
    }

    unmarkDirty() {
        this.dirty = false;
        this.clickInPopup = false;
        this.changesQueue.queueAttributeChange('dirty', this.dirty);
        this.getInputGroupElement().style.border = 'none';
    }

    handleRawDataChange(ev) {
        const content = ev.target.value;
        if (content === '') {
            this.rawValue = null;
        } else {
            this.rawValue = content;
        }
        this.changesQueue.queueAttributeChange('rawValue', this.rawValue);
        this.fireEventWithLock('onChange', this.onChange);
    }

    handleTextInputChange(ev) {

        function isCtrlV(ev: any) {
            let code = ev.keycode || ev.which;
            return code === 86 && ev.ctrlKey;
        }

        function isPrintableKey(ev: any) {
            let code = ev.keycode || ev.which;
            if(code >= 96 && code <= 105) return true;
            else if (code === 13 || code === 8) return true;
            else if(code === 46) return true;
            else if(ev.location > 0) return false;
            else if(code === 8) return true;
            else if(code < 48) return false;
            else if(code > 90) return false;
            else return true;
        }

        if(isPrintableKey(ev) || isCtrlV(ev)) {
            this.markDirty();
            if (this._writingDebaunceTimer) {
                clearTimeout(this._writingDebaunceTimer);
                this._writingDebaunceTimer = undefined;
            }

            const openSearch = () => {
                console.log(this.input, document.activeElement, this.input === document.activeElement)
                if (this.dirty && this.input === document.activeElement && !this.clickInPopup) {
                    if (ev.target.value === '') {
                        this.rawValue = null;
                    }
                    this.changesQueue.queueAttributeChange('searchRequested', true);
                    if (!this.popupOpen) {
                        this.popupOpen = true;
                    }
                    this.isSearch = true;
                    this.fireEvent('onClickSearchIcon', 'search');
                    this.crateTooltip($("div.search-icon", this.getInputGroupElement())[0])
                }
            }

            if (!this.popupOpen) {
                this._writingDebaunceTimer = setTimeout(openSearch, 800);
            }
        }
    }

    private setClickInPopup = (arg: boolean = true) => {
        this.clickInPopup = arg;
    }

    handleTextInputBlur(ev) {
        if (this.dirty && !this.clickInPopup) {
            if (ev.target.value === '' && this.rawValue) {
                this.fireEventWithLock('recordSelected', null);
                this.unmarkDirty();
            } else {
                this.fireEventWithLock('dictionaryComboValidate', JSON.stringify({id: this.guuid, code: ev.target.value}));
                ev.preventDefault();
            }
        } else if (this.clickInPopup) {
            this.clickInPopup = false;
            this.defferFunction(() => {
                document.getElementById(this.input.id).focus();
            });
        } else {
            this.getInputGroupElement().style.border = 'none';
        }
        ev.preventDefault();
    }

    protected getPopupProps(): RenderPopupProps {
        if(!!this.lastValueHtmlElement) {
            return {
                hookElementId: this.lastValueHtmlElement.id,
                parent: this.lastValueHtmlElement,
                position: 'left'
            }
        }
        return {
            hookElementId: this.getInputGroupElement().id,
            parent: this.getInputGroupElement(),
            position: this.isSearch ? 'left' : 'right'
        }
    }

    async renderPopup() {
        const { hookElementId, parent, position } = this.getPopupProps();

        const handlePopupClose = (force?: boolean) => {
            if ((!this.dirty && !this.clickInPopup) || force) {
                this.popupOpen = false;
                this.renderPopup();
            }
        }
        window['handlePopupClose'] = handlePopupClose;
        ReactDOM.render(createElement(DictionaryComboFhDPPopperTable, {
            title: this.title,
            columns: this.columns,
            rows: this.rows,
            readOnly: this.accessibility === "VIEW",
            hookElementId,
            isOpen: this.popupOpen,
            currentPage: this.page,
            pagesCount: this.pagesCount,
            parent,
            backgroundColor: this.popupColor,
            position,
            paginationPlacement: this.paginationPlacement,
            fireChangePopupEvent: (attr: {name: string, arg: any}[], event?: string) => {
                if (['nextPage', 'prevPage'].indexOf(event) > -1) {
                    this.clickInPopup = true;
                    this.pageChangeClicked = true;
                }
                for (const att of attr) {
                    this.changesQueue.queueAttributeChange(att.name, att.arg);
                    this[att.name] = att.arg;
                }
                this.fireEventWithLock(event || 'onChange', this.onChange);
            },
            handleClose: handlePopupClose,
            recordClick: (record: any) => {
                this.clickInPopup = true;
                if (this.dirty || this.input.value === '' || (this.input.value !== record.value && !this.input.value.startsWith(record.value))) {
                    this.input.value = record.value || '';
                    this.rawValue = record.value;
                    this.fireEventWithLock('recordSelected', record.value);
                    handlePopupClose(true);
                    this.fireEventWithLock('onChange', this.onChange);
                } else if (!this.dirty && (this.input.value === record.value || this.input.value.startsWith(record.value))) {
                    handlePopupClose(true);
                }
                this.unmarkDirty();
            },
            clickInPopup: this.setClickInPopup,
            translate: (string: string, args?: any, code?: string) => this.i18n.translateString(string, args, code || this.languageWrapped),
        }), this.divTooltip);
        this.clickInPopup = false;
    }

    protected getOldValueHtmlElement(): HTMLElement {
        return this.isTableMode ? this.lastValueHtmlElement : this.getInputGroupElement();
    }

    crateTooltip(element, isOldValue: boolean = false) {
        if(isOldValue) {
            const fBase = () => {
                this.isSearch = false;
                this.fireEvent('onClickLastValue', "search");
            }
            this.handleCrateTooltip(element, fBase);
            return;
        }

        const fBase = () => {
            const isSearch = $(element).is($($("div.search-icon", this.getInputGroupElement())[0]))
            const isOldValue = $(element).is($($("span.input-old-value", this.getOldValueHtmlElement())[0]))

            if (isSearch) {
                this.isSearch = true;
                this.fireEvent('onClickSearchIcon', "search");
            }
            if (isOldValue) {
                this.isSearch = false;
                this.fireEvent('onClickLastValue', "search");
            }
        }
        this.handleCrateTooltip(element, fBase);
    }

    protected handleCrateTooltip(element, fBase: () => void){
        let allTooltips = document.getElementsByClassName("dc-element");
        if(allTooltips.length >0){
            for(var i=0; i< allTooltips.length;i++){
                allTooltips[i]
                if (!allTooltips[i].classList.contains("hidden-popper") && allTooltips[i].id != this.divTooltipId.toString()){
                    allTooltips[i].classList.add("hidden-popper");
                }
            }
        }
        this.popupOpen = true;

        if (element && this.accessibility === "VIEW") {
            fBase();
        }
        this.renderPopup();
    }

    destroy(removeFromParent){
        ReactDOM.unmountComponentAtNode(this.divTooltip);
        if (this.popupOpen) {
            this.popupOpen = false;
            this.renderPopup();
        }
        if (this.instance) {
            this.instance.destroy();
            this.divTooltip.classList.add('hidden-popper');
            this.instance = null;
        }

        this.i18n.unsubscribe(this);
        super.destroy(removeFromParent);
    }

    languageChanged(code: string) {
        this.languageWrapped = code;
        if (this.popupOpen) {
            this.fireEvent('onClickSearchIcon', 'search');
            this.crateTooltip($("div.search-icon", this.getInputGroupElement())[0])
        }
        this.renderPopup();
    }

    update(change) {
        super.update(change);
        if (change.changedAttributes) {
            let shouldRender = false;
            $.each(change.changedAttributes, function (name, newValue) {
                switch (name) {
                    case 'language':
                        this.languageWrapped = newValue;
                        shouldRender = true;
                        break;
                    case 'columns':
                        this.columns = newValue;
                        shouldRender = true;
                        break;
                    case "codeValue":
                        this.codeValue = newValue;
                        break;
                    case "lastValue":
                        this.lastValue = newValue;
                        if(this.isTableMode && null != this.lastValueGroupSpanElement) {
                            if(this.componentObj.newValueText && (newValue == null || newValue.length == 0)) {
                                this.lastValueGroupSpanElement.innerText = this.newValueText;
                                this.lastValueGroupSpanElement.classList.remove('input-old-value');
                                this.lastValueGroupSearchElement.classList.add('d-none');
                            } else {
                                this.lastValueGroupSpanElement.innerText = this.lastValueParser ? this.lastValueParser(this.lastValue) : this.lastValue;
                                this.lastValueGroupSpanElement.classList.add('input-old-value');
                                this.lastValueGroupSearchElement.classList.remove('d-none');
                            }

                            this.toogleLastValueElement(this.rawValue == this.lastValue);
                        }
                        break;
                    case 'valueFromChangedBinding':
                        if (this.isTableMode) {
                            this.handleUpdateRawValueTableMode(newValue);
                            break;
                        }

                        if (newValue === 'null' || newValue === '') {
                            this.rawValue = null;
                            this.input.value = '';
                            this.unmarkDirty();
                            this.valueFromChangedBinding = newValue;
                        } else {
                            this.rawValue = newValue;
                            this.input.value = newValue || '';
                            this.unmarkDirty();
                            this.valueFromChangedBinding = newValue || '';
                        }
                        this.toogleLastValueElement(this.isTheSameLastValue());
                        break;
                    case 'shouldCloseTooltip':
                        this.shouldCloseTooltip = newValue;
                        if(this.shouldCloseTooltip == true){
                            this.destroy();
                        }
                        break;
                    case 'text':
                        console.log('TEXT!', newValue);
                        break;
                    case 'title':
                        this.title = this.fhml.resolveValueTextOrEmpty(newValue);
                        shouldRender = true;
                        break
                    case 'displayedComponentId':
                        this.displayedComponentId = newValue;
                        break;
                    case 'pagesCount':
                        this.pagesCount = newValue;
                        shouldRender = true;
                        break;
                    case 'page':
                        this.page = newValue;
                        shouldRender = true;
                        break;
                    case 'searchRequested':
                        this.searchRequested = newValue;
                        shouldRender = true;
                        break;
                    case 'rows':
                        this.rows = newValue;
                        shouldRender = true;
                        break;
                    case 'accessibility':
                        if(this.accessibility != newValue && newValue == 'EDIT'){
                            this.fireEvent('setGuuid', JSON.stringify({id: this.guuid}));
                        }
                        break;
                }
            }.bind(this));
            if(shouldRender) {
                this.renderPopup();
            }
        }

    }

    private handleUpdateRawValueTableMode(newValue: string): void {
        this.rawValue = newValue;
        this.input.textContent = newValue || "";
        this.unmarkDirty();
        this.valueFromChangedBinding = newValue;

        if(newValue === 'null' || newValue === '') {
            this.inputGroupElement.classList.add('hide-old-value');
        } else {
            this.inputGroupElement.classList.remove('hide-old-value');
        }

        this.toogleLastValueElement(this.rawValue == this.lastValue);
    }

    protected isTheSameLastValue(): boolean {
        return this.codeValue == this.lastValue;
    }

    updateModel() {
       super.updateModel();
       this.toogleLastValueElement(this.isTheSameLastValue());
    }

    extractChangedAttributes() {

        let comboAttrs = super.extractChangedAttributes();

        return comboAttrs;
    };

    onClickEvent(event) {
        event.stopPropagation();
        this.isLastValueTooltip = true;
        this.popupOpen = true;
        this.isSearch = true;
        this.changesQueue.queueAttributeChange('searchRequested', true);
        if(this.accessibility !== "VIEW") {
            if (this._formId === 'FormPreview') {
                this.fireEvent('onClickLastValue', "preview");
            } else {
                this.fireEventWithLock('onClickLastValue', "search");
            }
        }

        if(this.isTableMode) {
            this.crateTooltip($("div.search-icon", this.lastValueHtmlElement)[0], true);
        } else {
            this.crateTooltip($("div.input-group-append > span.input-old-value", this.htmlElement)[0], true);
        }
        event.target.blur();
    }

    onClickSearchIconEvent(event) {
        if(this.accessibility === "VIEW") {
            return;
        }

        event.stopPropagation();
        this.popupOpen = true;
        this.isSearch = true;
        this.isLastValueTooltip = false;
        if (this._formId === 'FormPreview') {
            this.fireEvent('onClickSearchIcon', "preview");
        } else {
            this.fireEvent('onClickSearchIcon', "search");
        }
        this.crateTooltip($("div.search-icon", this.getInputGroupElement())[0]);
        event.target.blur();
    }

    defferFunction(func) {
        setTimeout(func, 100);
    }
}

export {DictionaryComboFhDP}
