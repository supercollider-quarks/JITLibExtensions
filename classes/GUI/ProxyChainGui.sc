ProxyChainGui : JITGui {
	var <guiFuncs; // move to classvar!
	var <butZone, <buttonSpecs, <buttons, <namedButtons, <specialButtons, <editGui;

	*new { |chain, numItems = 16, parent, bounds, makeSkip = true, options|

		options = options ?? { if (chain.notNil) { chain.slotNames.asArray } };
		^super.new(nil, numItems, parent, bounds, makeSkip, options)
			.chain_(chain)
	}

	accepts { |obj| ^(obj.isNil or: { obj.isKindOf(ProxyChain) }) }

	chain_ { |chain| ^this.object_(chain) }
	chain { ^object }

	setDefaults { |options|
		if (parent.isNil) {
			defPos = 610@260
		} {
			defPos = skin.margin;
		};
		minSize = 510 @ (numItems * skin.buttonHeight + (skin.headHeight * 2));
	//	"minSize: %\n".postf(minSize);
	}

	makeViews { |options|

		namedButtons = ();
		specialButtons = ();


		options = options ?? { if(object.notNil) { object.slotNames.asArray } };

		guiFuncs =  (
			btlabel: { |but, name| but.states_([[name, Color.black, Color(1, 0.5, 0)]]) },
			label: { |but, name| but.states_([[name, Color.white, Color(1, 0.5, 0)]]) },
			slotCtl: { | but, name, level|
				var srcDict = ProxyChain.sourceDicts[name];
				var defLevel = level ?? { srcDict !? { srcDict[\level] } } ? 1;

				but.states_([["[" + name + "]"], [name, Color.black, Color.green(5/7)], ]);
				but.action_({ |but|
					[
						{
							this.chain.remove(name);
							this.chain.proxy.cleanNodeMap;
						},
						{ this.chain.add(name, defLevel.value) }
					][but.value].value
				});
			},

			extra: { |but, name, func|
				but.states_([[name, Color.black, Color(1, 0.7, 0)]]);
				but.action_(func);
			}
		);

		butZone = CompositeView(zone, Rect(0,0, 110, bounds.height - (skin.margin.y * 2)));
		butZone.addFlowLayout(2@2, 1@0);
		buttons = numItems.collect { Button.new(butZone, Rect(0,0, 100, skin.buttonHeight)).states_([["-"]]); };

		this.setButtons(options.asArray);

		this.makeEditGui;
	}

	makeEditGui { editGui = NdefGui(nil, numItems, zone); }

	setButtons { |specs|

		var objSlotNames = if (object.notNil) { object.slotNames.asArray } { [] };

		specs = (specs ? []);
		if (specs.size > buttons.size) {
			"ProxyChainGui: out of buttons... fix later".postln;
		};

		buttons.do { |but, i|
			var name, kind, funcOrLevel, setupFunc;
			var list = specs[i];
			but.visible_(list.notNil);

			if (list.notNil) {
				#name, kind, funcOrLevel, setupFunc = list.asArray;
				kind = kind ? \slotCtl;
				if (kind == \slotCtl) {
					namedButtons.put(name, but);
				} {
					specialButtons.put(name, but);
				};
				if (name.notNil) {
					guiFuncs[kind].value(but, name, funcOrLevel);
					setupFunc.value(this, but);
				};
				but.enabled_(name.notNil);
			}
		};

		buttonSpecs = specs;
	}

	getState {
		var state = (object: object, slotsInUse: [], slotNames: []);
		if (object.notNil) {
			state
				.put(\slotsInUse, object.slotsInUse.asArray)
				.put(\slotNames, object.slotNames.asArray)
		};
		^state
	}

	checkUpdate {
		var newState = this.getState;

		if (newState[\object].isNil) {
			this.name_('none');
			editGui.object_(object);
			butZone.enabled_(false);

			prevState = newState;
			^this
		};

		if (newState == prevState) { ^this };

		if (newState[\object] != prevState[\object]) {
			this.name_(object.key);
			butZone.enabled_(true);
			editGui.object_(object.proxy);
			editGui.name_(object.key);
		} {
			editGui.checkUpdate;
		};

		if (newState[\slotNames] != prevState[\slotNames]) {
		//	"new slotnames: ".post; newState[\slotNames].postcs;

			namedButtons.clear;

			buttons.select { |bt| specialButtons.includes(bt).not }.do { |but, i|
				var newName = newState[\slotNames][i];
				but.states_(but.states.collect(_.put(0, newName ? "-")));
				but.visible = newName.notNil;
				but.refresh;
				if (newName.notNil) {
					namedButtons.put(newName, but)
				}
			};

			object.slotNames.do { |name, i|
				editGui.addReplaceKey(("wet" ++ i).asSymbol, name, \amp.asSpec);
				editGui.addReplaceKey(("mix" ++ i).asSymbol, name, \amp.asSpec);
			};
		};

		if (newState[\slotsInUse] != prevState[\slotsInUse]) {
			namedButtons.keysValuesDo { |name, but|
				but.value_(newState[\slotsInUse].includes(name).binaryValue);
			}
		};

		prevState = newState;
	}
}

MainFXGui : ProxyChainGui {

	accepts { |obj| ^(obj.isNil or: { obj.isKindOf(MainFX) }) }

	name_ { |name|
		if (hasWindow) { parent.name_(name.asString) };
	}

	makeEditGui {
		var editGuiOptions = [ 'CLR', 'reset', 'doc', 'fade', 'wake', 'end', 'pausR', 'sendR' ];
		editGui = NdefGui(nil, numItems, zone, bounds: 400@0, options: editGuiOptions);
	}
}

MasterFXGui : MainFXGui {

	*new { |chain, numItems = 16, parent, bounds, makeSkip = true, options|
		"MasterFX has been renamed MainFX, and MasterFXGui has been renamed MainFXGui.\n"
		"Please adapt your code accordingly.".postln;
		^MainFXGui(chain, numItems, parent, bounds, makeSkip, options);
	}

}
