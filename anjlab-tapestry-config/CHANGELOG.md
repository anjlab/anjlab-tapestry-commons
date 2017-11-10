1.1.1
=====
 - Added support for optional prefix for property names when extending configs.

1.1.0
=====
 - Changed default encoding of properties files to UTF-8

1.0.1
=====
 - Fixed a bug when Tapestry IoC could pick wrong constructor for service creation.

1.0.0
=====

* Initial version based on version 1.6.4 of `anjlab-tapestry-commons` extracted as its own library, with the following changes:

  - Upgraded to Tapestry 5.4.2
  - `ConfigHelper` package renamed to `com.anjlab.tapestry5.config.ConfigHelper`
  - `ConfigHelper#names()` renamed to `ConfigHelper#getPropertyNames()`
  - `ConfigHelper#get()` renamed to `ConfigHelper#getRaw()`
  - `anjlab-tapestry-config` is now a drop-in module that will register itself
  - Added support for config validators, built-in validators are: "UnreferencedProperties", "PropertyType"