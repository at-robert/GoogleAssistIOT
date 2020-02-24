load('api_config.js');
load('api_events.js');
load('api_gcp.js');
load('api_gpio.js');
load('api_mqtt.js');
load('api_timer.js');
load('api_sys.js');
load('api_rpc.js');

/*
    LED function
*/
let led = Cfg.get('board.led1.pin');              // Built-in LED GPIO number

function led_init()
{
    led_set(false);
}

function led_set(enable)
{
    GPIO.write(led, !enable);
}

/* 
    Google pub/sub 
*/

let FANSPEED_LOW = 'Low';
let FANSPEED_MEDIUM = 'Medium';
let FANSPEED_HIGH = 'High';

let state = {FanSpeed: "", OnOff: false};  // Device state
let topic = '/devices/' + Cfg.get('device.id') + '/config';

MQTT.sub(topic, function(conn, topic, msg) {
    print('Topic:', topic, 'message:', msg);
	
    let obj = JSON.parse(msg);

    state.OnOff = obj.OnOff;
    state.FanSpeed = obj.FanSpeed;
        
    if(state.OnOff === true)
    {
        if(state.FanSpeed === FANSPEED_HIGH)
        {
            print("FanSpeed: High");
        }
        else if(state.FanSpeed === FANSPEED_MEDIUM)
        {
            print("FanSpeed: Medium");
        }
        else if(state.FanSpeed === FANSPEED_LOW)
        {
            print("FanSpeed: Low");            
        }   
    }
    else
    {
        print("POWER OFF");
    }
}, null);

/* 
    Button handler 
*/

let btn = 0;
let btn_count = 0;

function button_init()
{
    let btnPull, btnEdge;
    
    btn = Cfg.get('board.btn1.pin'); // Built-in button GPIO
    
    if (button_available())  // To check button hardware is available
    {   
        print('button_init');
        
        /* Setup button parameter */
        if (Cfg.get('board.btn1.pull_up') ? GPIO.PULL_UP : GPIO.PULL_DOWN) {
            btnPull = GPIO.PULL_UP;
            btnEdge = GPIO.INT_EDGE_NEG;
        } else {
            btnPull = GPIO.PULL_DOWN;
            btnEdge = GPIO.INT_EDGE_POS;
        }

        /* Button handler */
        GPIO.set_button_handler(btn, btnPull, btnEdge, 20, function() {

            button_handler();

        }, null);
    }    
}

function button_available()
{
    if (btn >= 0) 
    {
        return true;
    }
    
    return false;
}

function button_handler()
{
    print('button_handler');
    
	btn_count = btn_count + 1;
    if(btn_count=== 1)
	{
		state.OnOff = true;
		state.FanSpeed = FANSPEED_LOW;
	}else if(btn_count=== 2){
		state.OnOff = true;
		state.FanSpeed = FANSPEED_MEDIUM;
	}else if(btn_count=== 3){
		state.OnOff = true;
		state.FanSpeed = FANSPEED_HIGH;
	}else{
		state.OnOff = false;
		state.FanSpeed = FANSPEED_LOW;
		btn_count= 0;
	}  
    let message = JSON.stringify(state);    
    let pub_topic = '/devices/' + Cfg.get('device.id') + '/state';
    print('== Publishing to ' + pub_topic + ':', message);
    MQTT.pub(pub_topic, message, 1 /* QoS */);    
    
	/*    
	let f = ffi('int my_func(int, int)');
	print('Calling C my_func:', f(1,2));
		
	let f2 = ffi('void amtran_wifi_test(void)');
	f2();
	*/ 
}

/* 
    Main function 
*/

let timer_count = 0;
let timer_toggle = false;

led_init();
button_init();

Timer.set(100, Timer.REPEAT, function() {
    
    timer_count++;
    
    if(state.OnOff === true)
    {
        if(state.FanSpeed === FANSPEED_HIGH)
        {
            if(timer_count % 1 === 0)
            {
                if(timer_toggle === true)
                {
                    timer_toggle = false;
                }
                else
                {
                    timer_toggle = true;
                }
                led_set(timer_toggle);
            }          
        }
        else if(state.FanSpeed === FANSPEED_MEDIUM)
        {
            if(timer_count % 4 === 0)
            {
                if(timer_toggle === true)
                {
                    timer_toggle = false;
                }
                else
                {
                    timer_toggle = true;
                }
                led_set(timer_toggle);
            }

        }
        else if(state.FanSpeed === FANSPEED_LOW)
        {
            if(timer_count % 10 === 0)
            {
                if(timer_toggle === true)
                {
                    timer_toggle = false;
                }
                else
                {
                    timer_toggle = true;
                }
                led_set(timer_toggle);
            }            
        }   
    }
    else
    {
        if(timer_count % 15 === 0)
        {
            led_set(false);
        }
    }
}, null);