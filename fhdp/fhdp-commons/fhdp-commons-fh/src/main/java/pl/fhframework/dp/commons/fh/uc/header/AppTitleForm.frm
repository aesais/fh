<Form container="appTitle" id="appTitleInner" styleClasses="form-transparent" xmlns="http://fh.asseco.com/form/1.0">
    <AvailabilityConfiguration>
        <Invisible when="declarationOfAccessibilityHidden==true">accessibilityLnk</Invisible>
    </AvailabilityConfiguration>
    <OutputLabel width="md-12" value="{appName}" styleClasses="pb-0"/>
    <HtmlView id="version" text="{version}" styleClasses="font-weight-bold"/>
    <Link id="accessibilityLnk" url="{declarationOfAccessibilityUrl}" value="{$.fhdp.menu.ui.declaration.of.accessibility.help}" styleClasses="font-weight-normal mt-2" newWindow="true"/>
</Form>