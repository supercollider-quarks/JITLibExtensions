/*
// convenience methods:
g = Ndef(\x, { GrayNoise.ar(0.2) }).play(vol: 0.5).gui;
g.addMeter
g.removeMeter

NdefMixer(s).addMeter;
m = MonitorGui(Ndef(\x));
m.addMeter;
m.removeMeter

// test vertical slider layout in PrePostView:
m = MonitorGui(Ndef(\x), bounds: 100@300);
m.addMeter;
m.removeMeter;

*/

+ NdefGui {
	addMeter { ProxyMeter.addNdefGui(this) }
	removeMeter { ProxyMeter.removeNdefGui(this) }
}

+ ProxyMixer {
	addMeter { ProxyMeter.addMixer(this) }
	removeMeter { ProxyMeter.removeMixer(this) }
}

+ MonitorGui {
	addMeter { ProxyMeter.addMonitorGui(this) }
	removeMeter { ProxyMeter.removeMonitorGui(this) }
}
