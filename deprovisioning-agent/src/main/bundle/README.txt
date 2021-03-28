Steps to be performed on Unity side before starting the deprovisioning agent:

1. Import attribute types from file deprovisioningAttrTypes.json
2. Create message template compatible with UserNotification with name the same as in 
   deprovisioning application configuration (unity.email.template key).
   When defining this template you can use the following variables:
	  a. email - technical admin email of user's home IdP
	  b. daysLeft - number of days left to planned user removal
	  c. deprovisioningDate - date when removal will take place