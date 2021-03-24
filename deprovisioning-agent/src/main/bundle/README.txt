Steps to be performed on the side of the unity system before starting the deprovisioning agent:
1.Import attribute type from file deprovisioningAttrTypes.json
2.Create message template compatible with UserNotification with name the same as in deprovisioning application configuration (unity.email.template key).
  Defining this template you can use custom variables:
	  a.email - technical admin email 
	  b.daysLeft - number of days left to deprovisioning
	  c.deprovisionigDate - deprovisionig date