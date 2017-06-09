1.0.0
=====

* Initial version based on version 1.6.4 of `anjlab-tapestry-commons` extracted as its own library, with the following changes:

  - Upgraded to Tapestry 5.4.2
  - `ConfigHelper` package renamed to `com.anjlab.tapestry5.config.services.ConfigHelper`
  - `anjlab-tapestry-config` is now a drop-in module that will register itself
  - Added support for config validators, one built-in validator "UnreferencedProperties" was added