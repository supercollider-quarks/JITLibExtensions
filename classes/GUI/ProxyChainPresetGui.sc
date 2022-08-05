

ProxyChainPresetGui : JITGui {
	var <pcGui, <pcPrePop, <currPresetName, <currPresetIndex;

	accepts { |obj|
		^obj.isNil or: { obj.isKindOf(ProxyChainPreset) };
	}

	setDefaults { |options|
		if (parent.isNil) {
			defPos = 610@260
		} {
			defPos = skin.margin;
		};
		minSize = 510 @ (numItems * skin.buttonHeight + (skin.headHeight * 2));
		"minSize: %\n".postf(minSize);
	}

	makeViews { |options|
		this.makeTopLine;
		this.makeEditGui;
	}

	makeEditGui { pcGui = ProxyChainGui(nil, numItems, zone, makeSkip: false) }

	makeTopLine {
		pcPrePop =  EZPopUpMenu(zone, Rect(0,0,150, 20), \PCPre, [],
			{ |ez|
				currPresetName = ez.item;
				currPresetIndex = object.settingNames.indexOf(currPresetName);
				object.setCurr(currPresetName)
			},
			labelWidth: 50
		);
		// make buttons:
		[
			'<', { "load prev preset".postcln;
				object.stepCurr(-1);
				// gui_updateListItem.();
			},
			'?', {
				"load rand preset".postcln;
				object.stepCurr((object.settingNames.size-1).rand+1);
				// gui_updateListItem.();
			},
			'>', { "load next preset".postcln;
				object.stepCurr(1);
				// gui_updateListItem.();
			},
			\sto, { |but|
				object.settings.storeDialog('???',
					object.getCurr,
					but.absoluteBounds.center.postcs);
			},
			\disk, { object.settings.write; },
			\open, { (object.settings.dirpath +/+ object.settings.dirname).openOS; },
			\del, { |but| object.settings.deleteDialog(but.absoluteBounds.center) }

		].keysValuesDo { |key, fun|
			var width = key.asString.size.max(3) * 14;
			Button(zone, Rect(0,0,width,20)).states_([[key]])
			.action_(fun)
		};

	}

	getState {
		object ?? { ^() };

		^(
			object: object,
			settingNames: object.settingNames,
			currSet: object.currSet // index of current setName in settings
		)
	}

	checkUpdate {
		var newState = this.getState;
		var newObj = newState[\object];
		var newChain, newNames, newSetName;

		if (newObj != prevState[\object]) {
			if (newObj.notNil) { newChain = newObj.chain };
			pcGui.object = newChain
		};

		pcGui.checkUpdate;

		if (object.isNil) {
			pcPrePop.items = [];
			^this;
		};
		// we have an object:
		newNames = newState[\settingNames];

		if (newNames != prevState[\settingNames]) {
			pcPrePop.items = newNames;
			newState.put(\settingNames, newNames.copy);
		};

		if (newState[\currSet] != prevState[\currSet]) {
			pcPrePop.value = newState[\currSet];
		};

		prevState = newState;
	}

}