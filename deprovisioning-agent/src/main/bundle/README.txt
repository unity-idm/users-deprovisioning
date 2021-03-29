Steps to be performed on Unity side before starting the deprovisioning agent:

0. Make sure to record SAML persistent identity as an identifier identity in Unity (via input translation profile)
1. Import attribute types from file deprovisioningAttrTypes.json (Directory setup -> Attribute types in Console)
2. Create message template compatible with UserNotification with the same name as used in 
   deprovisioning application configuration (unity.email.template key).
   When defining this template you can use the following variables:
	  a. custom.email - technical admin email of user's home IdP
	  b. custom.daysLeft - number of days left to planned user removal
	  c. custom.deprovisioningDate - date when removal will take place
	  