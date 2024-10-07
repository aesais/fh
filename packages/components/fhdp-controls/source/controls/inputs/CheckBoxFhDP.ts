import {CheckBox} from 'fh-basic-controls';
import {FhContainer, HTMLFormComponent} from 'fh-forms-handler';
import getDecorators from "inversify-inject-decorators";

let {lazyInject} = getDecorators(FhContainer);

class CheckBoxFhDP extends CheckBox {
  protected isDefaultStyle: boolean = true;
  protected isTriState: boolean = true;
  protected isIntermediate: boolean = false;

  constructor(componentObj: any, parent: HTMLFormComponent) {
    super(componentObj, parent);
    if (this.componentObj.rawValue === 'undefined' || this.componentObj.rawValue === undefined || this.componentObj.rawValue === 'null' || this.componentObj.rawValue === null) {
      this.rawValue = null;
    } else {
      this.rawValue = this.componentObj.rawValue == true || this.componentObj.rawValue == "true";
    }

    if(componentObj.isDefaultStyle === false || componentObj.isDefaultStyle === 'false'){
      this.isDefaultStyle = false;
    }
    if(componentObj.isTriState === false || componentObj.isTriState === 'false') {
      this.isTriState = false;
    }
  }

  create() {
    super.create();
    if(this.isDefaultStyle) {
      this.htmlElement.classList.add('form-switch');
    }

    // if(this.isTriState) {
    //   $(this.component).on('change', this.onChangeTriState.bind(this));
    // }

    if(this.rawValue == null) {
      if(this.isTriState) {
        this.input.indeterminate = true;
        this.isIntermediate = true;
        this.input.checked = false;
        this.changesQueue.queueValueChange(undefined); //Clear changes that was set by parent checkbox.
      }
    }

  }

  inputCheckEvent() {
    if(this.isTriState) {
      if (this.input.checked === false && this.input.indeterminate === false) {
        this.input.indeterminate = true;
        this.isIntermediate = true;
        this.input.checked = null;
        this.rawValue = '';
        this.changesQueue.queueValueChange(null);

      } else if (this.isIntermediate === true) {
        this.input.indeterminate = false;
        this.isIntermediate = false;
        this.input.checked = false;
        this.rawValue = false;
        this.changesQueue.queueValueChange(this.input.checked);
      } else {
        this.input.indeterminate = false;
        this.isIntermediate = false;
        this.input.checked = true;
        this.rawValue = true;
        this.changesQueue.queueValueChange(this.input.checked);
      }
    } else {
      this.changesQueue.queueValueChange(this.input.checked);
    }

  }


  update(change) {
    super.update(change);
    if(this.isTriState){
      $.each(change.changedAttributes || [], function (name, newValue) {
        switch (name) {
          case 'rawValue':
            if(newValue !== this.rawValue) {
              if (newValue == "" || newValue == null || newValue == '') {
                this.input.checked = null;
                this.isIntermediate = true;
                this.input.indeterminate = true;
                this.rawValue = null;

              } else {
                this.rawValue = (newValue == true) || (newValue == "true");
                this.input.checked = this.rawValue;
                this.isIntermediate = false;
                this.input.indeterminate = false;

              }
              if (this.input.disabled == false) {
                this.changesQueue.queueValueChange(undefined);
              }
            }



            break;
        }
      }.bind(this));
    }
  }

  extractChangedAttributes() {
    return this.changesQueue.extractChangedAttributes();
  };

  destroy(removeFromParent: boolean) {
    super.destroy(removeFromParent);
  }
}

export {CheckBoxFhDP}
