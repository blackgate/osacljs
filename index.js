var global = this;

ObjC.import('Foundation');

var fm = $.NSFileManager.defaultManager;

var require = function (path) {
    var contents = fm.contentsAtPath(path.toString()); // NSData
    contents = $.NSString.alloc.initWithDataEncoding(contents, $.NSUTF8StringEncoding);

    var module = {exports: {}};
    var exports = module.exports;
    
    eval(ObjC.unwrap(contents));

    return module.exports;
};

require('./out/main');
