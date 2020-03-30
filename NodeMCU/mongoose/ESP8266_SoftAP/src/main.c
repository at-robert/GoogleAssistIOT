/*
 * Copyright (c) 2014-2018 Cesanta Software Limited
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the ""License"");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an ""AS IS"" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "mgos.h"
#include "mgos_rpc.h"
#include "mgos_rpc_channel_udp.h"

static void sum_cb(struct mg_rpc_request_info *ri, void *cb_arg,
                   struct mg_rpc_frame_info *fi, struct mg_str args);
				   

static void amtran_rpc_wifi_setup_sta_handler(struct mg_rpc_request_info *ri,
                                            void *cb_arg,
                                            struct mg_rpc_frame_info *fi,
                                            struct mg_str args);
											

static void amtran_rpc_gcp_setup_handler(struct mg_rpc_request_info *ri,
                                            void *cb_arg,
                                            struct mg_rpc_frame_info *fi,
                                            struct mg_str args);
//Joe add
static void joe_rpc_get_device_ip_handler(struct mg_rpc_request_info *ri,
                                            void *cb_arg,
                                            struct mg_rpc_frame_info *fi,
                                            struct mg_str args);
                                            
int mgos_print_sys_info_wifi_ip(struct json_out *out);

enum mgos_app_init_result mgos_app_init(void) {
    printf("mgos_app_init");
	
	struct mg_rpc *c = mgos_rpc_get_global();	
	
	// Somewhere in init function, register the handler:
	mg_rpc_add_handler(c, "AMT.Sum", "{a: %lf, b: %lf}", sum_cb, NULL);	
	
	mg_rpc_add_handler(c, "AMT.ConnectToAP", "{ssid: %Q, pass: %Q}",
					 amtran_rpc_wifi_setup_sta_handler, NULL);
					 
	mg_rpc_add_handler(c, "AMT.SetupGCP", "{enable: %B, project: %Q, region: %Q, registry: %Q}",
					 amtran_rpc_gcp_setup_handler, NULL);						 
    //Joe add
    mg_rpc_add_handler(c, "Joe.GetDeviceIP", "",
                     joe_rpc_get_device_ip_handler,NULL);
	return MGOS_APP_INIT_SUCCESS;
}
//Joe add
static void joe_rpc_get_device_ip_handler(struct mg_rpc_request_info *ri,
                                            void *cb_arg,
                                            struct mg_rpc_frame_info *fi,
                                            struct mg_str args){
    mg_rpc_send_responsef(ri, "%M", (json_printf_callback_t) mgos_print_sys_info_wifi_ip);
    //mg_rpc_send_responsef(ri, "123444");
  (void) cb_arg;
  (void) args;
  (void) fi;
}
int my_func(int a, int b) {
	return a + b;
}

void amtran_wifi_test(void) {
	struct mgos_config_wifi_sta cfg = {0};
	cfg.ssid = "VXA";
	cfg.pass = "12345678";
	cfg.enable = true;

	if (mgos_wifi_setup_sta(&cfg)) {
		printf("WIFI connect successed\r\n");
	} else {
		printf("WIFI connect failed\r\n");
	}
}


static void sum_cb(struct mg_rpc_request_info *ri, void *cb_arg,
                   struct mg_rpc_frame_info *fi, struct mg_str args) {
  double a = 0, b = 0;
  if (json_scanf(args.p, args.len, ri->args_fmt, &a, &b) == 2) {
    mg_rpc_send_responsef(ri, "%.2lf", a + b);
  } else {
    mg_rpc_send_errorf(ri, -1, "Bad request. Expected: {\"a\":N1,\"b\":N2}");
  }
  (void) cb_arg;
  (void) fi;
}


static void amtran_rpc_wifi_setup_sta_handler(struct mg_rpc_request_info *ri,
                                            void *cb_arg,
                                            struct mg_rpc_frame_info *fi,
                                            struct mg_str args) {
  char* err = NULL;
  struct mgos_config_wifi_sta cfg = {0};
  json_scanf(args.p, args.len, ri->args_fmt, &cfg.ssid, &cfg.pass);
  cfg.enable = true;
  
  mgos_sys_config_set_wifi_sta_enable(true);
  mgos_sys_config_set_wifi_sta_ssid(cfg.ssid);
  mgos_sys_config_set_wifi_sta_pass(cfg.pass);

  save_cfg(&mgos_sys_config, &err);

  if (mgos_wifi_setup_sta(&cfg)) {
      
    mg_rpc_send_responsef(ri,NULL);
  } else {
    mg_rpc_send_errorf(ri, -1, "%s failed", "mgos_wifi_setup_sta");
  }

  free((char *) err);
  free((char *) cfg.ssid);
  free((char *) cfg.pass);
  (void) fi;
  (void) cb_arg;
}

static void amtran_rpc_gcp_setup_handler(struct mg_rpc_request_info *ri,
                                            void *cb_arg,
                                            struct mg_rpc_frame_info *fi,
                                            struct mg_str args) {
												


	char* err = NULL;
	char* gcp_project;
	char* gcp_region;
	char* gcp_registry;
	const char* gcp_device;
	char* gcp_key;
	
	bool gcp_enable = false;
	bool mqtt_enable = false;
	
	//{enable: %B, project: %Q, region: %Q, registry: %Q}
	
	json_scanf(args.p, args.len, ri->args_fmt, &gcp_enable, &gcp_project, &gcp_region, &gcp_registry);


	mgos_sys_config_set_gcp_project(gcp_project);
	//printf("GCP Project: %s", mgos_sys_config_get_gcp_project());		
	
	mgos_sys_config_set_gcp_region(gcp_region);
	//printf("GCP Region: %s", mgos_sys_config_get_gcp_region());
	
	mgos_sys_config_set_gcp_registry(gcp_registry);
	//printf("GCP Registry: %s", mgos_sys_config_get_gcp_registry());
	
	gcp_device = mgos_sys_config_get_device_id();
	mgos_sys_config_set_gcp_device(gcp_device);
	//printf("GCP Device: %s", mgos_sys_config_get_gcp_device());
	
	gcp_key = (char*) malloc(32);
	snprintf(gcp_key, 32, "gcp-%s.key.pem", gcp_device);
	mgos_sys_config_set_gcp_key(gcp_key);
	//printf("GCP Device: %s", mgos_sys_config_get_gcp_key());
	
	mgos_sys_config_set_gcp_enable(gcp_enable);	
	//printf("GCP Enable: %s", mgos_sys_config_get_gcp_enable());
	
	mqtt_enable = gcp_enable;
	mgos_sys_config_set_mqtt_enable(mqtt_enable);	
	//printf("MQTT Enable: %s", mgos_sys_config_get_mqtt_enable());	
		
	save_cfg(&mgos_sys_config, &err);
	//printf("Saving configuration: %s\n", err ? err : "no error");
	
	mg_rpc_send_responsef(ri, NULL);	
	
	free((char *) err);
	free((char *) gcp_project);
	free((char *) gcp_region);
	free((char *) gcp_registry);	
	//free((char *) gcp_device);	
	free((char *) gcp_key);

	(void) fi;
	(void) cb_arg;
	
	mgos_system_restart_after(100);
}

//Joe add
int mgos_print_sys_info_wifi_ip(struct json_out *out) {
  struct mgos_net_ip_info ip_info;
  memset(&ip_info, 0, sizeof(ip_info));
#ifdef MGOS_HAVE_WIFI
  char *status = mgos_wifi_get_status_str();
  char *ssid = mgos_wifi_get_connected_ssid();
  char sta_ip[16], ap_ip[16];
  memset(sta_ip, 0, sizeof(sta_ip));
  memset(ap_ip, 0, sizeof(ap_ip));
  if (mgos_net_get_ip_info(MGOS_NET_IF_TYPE_WIFI, MGOS_NET_IF_WIFI_STA,
                           &ip_info)) {
    mgos_net_ip_to_str(&ip_info.ip, sta_ip);
  }
  if (mgos_net_get_ip_info(MGOS_NET_IF_TYPE_WIFI, MGOS_NET_IF_WIFI_AP,
                           &ip_info)) {
    mgos_net_ip_to_str(&ip_info.ip, ap_ip);
  }
#endif
  (void) ip_info;

  int len = json_printf(
      out,

#ifdef MGOS_HAVE_WIFI
      "{wifi: {sta_ip: %Q, ap_ip: %Q, status: %Q, ssid: %Q}}",
      sta_ip, ap_ip, status == NULL ? "" : status, ssid == NULL ? "" : ssid
#endif
  );
  printf("[Joe]ap_ip:%s\n",ap_ip);

#ifdef MGOS_HAVE_WIFI
  free(ssid);
  free(status);
#endif
  return len;
}
