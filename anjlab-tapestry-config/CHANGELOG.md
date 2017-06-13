1.0.0
=====

* Initial version based on version 1.6.4 of `anjlab-tapestry-commons` extracted as its own library, with the following changes:

  - Upgraded to Tapestry 5.4.2
  - `ConfigHelper` package renamed to `com.anjlab.tapestry5.config.ConfigHelper`
  - `ConfigHelper#names()` renamed to `ConfigHelper#getPropertyNames()`
  - `ConfigHelper#get()` renamed to `ConfigHelper#getRaw()`
  - `anjlab-tapestry-config` is now a drop-in module that will register itself
  - Added support for config validators, built-in validators are: "UnreferencedProperties", "PropertyType"