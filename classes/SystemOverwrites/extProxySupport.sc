/* non-gui functions related to (mainly Node)-Proxies and specs:

// true:
\wet1.isFilterRole  // true
\mix5.isFilterRole  // true
\wet.isFilterRole    //false
\mix5berta.isFilterRole // false
\wet2234d.isFilterRole  // false
*/

+ Symbol {
	// support for nodeproxy filter roles
	isFilterRole {
		var str, splitIndex, head, tail;
		str = this.asString;
		splitIndex = str.detectIndex(_.isDecDigit);
		if (splitIndex.isNil) { ^false };

		head = str.keep(splitIndex);
		// roles could be looked up somewhere
		if ([ "wet", "mix" ].any (_ == head).not) { ^false };

		tail = str.drop(splitIndex);
		^tail.every(_.isDecDigit);
	}
}

+ Spec {
	// for guis, guess a reasonable spec range if none given
	*guess { |key, value|

		if (key.isFilterRole) { ^[0, 1].asSpec };

		if (value.isKindOf(Array)) { value = value[0] };
		if (value.isKindOf(SimpleNumber).not) { ^nil };

		// label units as \guess so one can throw spec away later.
		^if (value.abs > 0) {
			ControlSpec(value/20, value*20, \exp, 0, value, \guess)
		} {
			ControlSpec(-2, 2, \lin, 0, value, \guess)
		};
	}
}

+ TaskProxy {
	// use \orderNames in halo for maintaining non-alphabetic order of names,
	// ananlog to NodeProxy, where controlKeys have an order.
	controlKeys {
		var cKeys = this.getHalo(\orderedNames);
		if (cKeys.notNil) { ^cKeys };
		cKeys = if (envir.notNil) { envir.keys(Array).sort } { [] };
		^cKeys;
	}
}
