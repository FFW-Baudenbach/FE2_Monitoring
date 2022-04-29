# FE2_Monitoring

The purpose of this dockerized Java SpringBoot application is to monitor the infrastructure 
of our local fire department, especially the [Alamos FE2](https://www.alamos-gmbh.com/service/fe2/)-System.  
But also specialties like if the printer paper tray is open are checked.   
The checks are done via Ping, HTTP(S) calls and concrete API usages.  
If an error occurred, a [Pushover](https://pushover.net/) message is sent. As well, after recovering, another notice is sent.  
The application can be started in internal and external mode:
* INTERNAL: Meant to run inside the fire department network and performs the actual checks
* EXTERNAL: Meant to run externally and only tests the health endpoint of the application from outside to ensure monitoring is running

## Dependencies

Some checks have dependencies to other applications like [FE2_Kartengenerierung](https://github.com/FFW-Baudenbach/FE2_Kartengenerierung),
[FE2_SmartHome](https://github.com/FFW-Baudenbach/FE2_SmartHome) 
and [FE2_ReverseProxy](https://github.com/FFW-Baudenbach/FE2_ReverseProxy).

## Implemented Checks
(this is also an example output on console and in pushover message)  

✅	FritzBox - Ping  
✅	FritzBox - WebUI  
❌	WindowsPC - Ping: Unreachable  
✅	RaspberryPi - Ping  
✅	FE2 Web - Direct  
✅	FE2 Web - Reverse proxy  
✅	FE2 Web - Http redirect  
✅	FE2 Web - External  
✅	FE2 Rest - External Status  
✅	FE2 Rest - Monitoring Status  
✅	FE2 Rest - Monitoring Inputs  
✅	FE2 Rest - Monitoring Cloud  
✅	FE2 Rest - Monitoring MQTT   
✅	FE2 Rest - Monitoring System  
✅	FE2_Kartengenerierung - Health  
✅	FE2_Kartengenerierung - Maps  
✅   FE2_SmartHome - Health: Actor is ON  
✅	Printer - Ping  
✅	Printer - WebUI  
✅	Printer - Device Status: running(2)  
✅	Printer - Black Toner: 64% remaining  
✅	Website - Access  
✅	Website - Http redirect  

## Daily aliveness check

Sends once in the morning a message about the current state.

## Pushover notification

If something goes wrong more than 5 times, a Pushover notification is sent to the admin.  
If it recovered more than 5 times in a row, a resolved Pushover message is also sent to the admin.

## Health endpoint

Exposes the last state via /actuator/health to enable external monitoring.

## REST API

Exposes the last performed check result via http.

## Note

The Tool is not generalized, all configurations (but secrets of course) are hardcoded.  
The timings can be configured as well or the defaults are used.  
  
The tool is designed to be as simple as possible for also not too experienced developers.

