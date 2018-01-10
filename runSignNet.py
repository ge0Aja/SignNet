import SignNet
import matlab
import sys

SignNet.initialize_runtime(['-nojvm'])
global signnet

def calc(x1,x2,x3,x4,x5,xacc1,xacc2,xacc3,y1,y2,y3,y4,y5,yacc1,yacc2,yacc3):
	signnet = SignNet.initialize()
	a = [x1,x2,x3,x4,x5,xacc1,xacc2,xacc3,y1,y2,y3,y4,y5,yacc1,yacc2,yacc3]
	print(a)
	res = signnet.SignNet(matlab.double(a))
	#print(res)
	return res
	
if __name__ == '__main__':
	calc(*sys.argv[1:])