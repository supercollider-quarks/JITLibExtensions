// repair a few annoyances in JITGuis
// quietly for 3.6.6 and earlier versions.
// remove when 3.6.6 is ancient.

// enable TaskProxyGuis for anonymous tasks,
// and let TdefGuis et al be more generous

+ TaskProxyGui {
	*observedClass { ^PatternProxy }
	accepts { |obj| ^obj.isNil or: { obj.isKindOf(PatternProxy) } }
}
+ PatternProxy {
	key { ^'anon' }
}

// fix invisible buttons on TdefGuis
// - offColor was Color.clear, now Color.grey(0.8, 0.5).
+ JITGui {
	*initClass {
		Class.initClassTree(GUI);

		GUI.skins.put(\jit, (
				fontSpecs: 	["Helvetica", 12],
				fontColor: 	Color.black,
				background: 	Color(0.8, 0.85, 0.7, 0.5),
				foreground:	    Color.grey(0.95),
				onColor:		Color(0.5, 1, 0.5),
				offColor:		Color.grey(0.8, 0.5),
				hiliteColor:	Color.green(1.0, 0.5),
				gap:			0 @ 0,
				margin: 		2@2,
				buttonHeight:	18,
				headHeight: 	24

			)
		);
	}

}

