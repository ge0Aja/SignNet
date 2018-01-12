from flask import Flask
#from runSignNet import calc
from flask import jsonify
from flask import request

import SignNet
import matlab

app = Flask(__name__)
SignNet.initialize_runtime(['-nojvm'])
signnet = SignNet.initialize()

@app.route('/postjson', methods = ['POST'])
def call_calc():
	print "A new connection requested"
	a = []
	print(request.is_json)
	content = request.get_json()
	res = calc(float(content['x1']),float(content['x2']),float(content['x3']),float(content['x4']),float(content['x5']),float(content['xacc1']),float(content['xacc2']),float(content['xacc3']),float(content['y1']),float(content['y2']),float(content['y3']),float(content['y4']),float(content['y5']),float(content['yacc1']),float(content['yacc2']),float(content['yacc3']))
	for _ in zip(*res): a.append(_)
	a = a[0]
	print(a)
	if(max(a) > 0.80):
		#return a json object
		return_val = {"status": "success","word":str(a.index(max(a)))}
	else:
		return_val = {"status": "unknown"}
	#return str(a.index(max(a)))
	return jsonify(return_val)
	
def calc(x1,x2,x3,x4,x5,xacc1,xacc2,xacc3,y1,y2,y3,y4,y5,yacc1,yacc2,yacc3):
	a = [x1,x2,x3,x4,x5,xacc1,xacc2,xacc3,y1,y2,y3,y4,y5,yacc1,yacc2,yacc3]
	#print(a)
	res = signnet.SignNet(matlab.double(a))
	#print(res)
	return res

app.run(host='0.0.0.0', port= 8090)
	