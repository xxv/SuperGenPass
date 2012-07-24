#!/usr/bin/python

# This was a quick hack for generating the star in the visual hash.

svg = 'M 1.7e-7,1037.1448 1.8773281,1042.9725 8,1042.9575 l -4.9624192,3.5863 1.9066909,5.8183 L -4e-8,1048.7502 -4.9442721,1052.3616 -3.0375809,1046.5433 -8,1042.957 l 6.1226718,0.015 z'

svg = 'M 2.2734424e-7,-8.4756811 1.8936006,-2.5973891 8.0693433,-2.6129598 3.0639101,1.0044533 4.9871282,6.873122 1.2638481e-8,3.2305143 -4.9871286,6.8731218 l 1.9232185,-5.8686685 -5.0054331,-3.6174135 6.1757426,0.015571 z'

mode = ''
obj = 'STAR'
for p in svg.split(' '):
    if p == 'M':
        mode = 'lineTo'
    elif p == 'm':
        mode = 'rLinTo'
    elif p == 'L':
        mode = 'lineTo'
    elif p == 'l':
        mode = 'rLineTo'
    elif p == 'z':
        pass
    else:
        (x,y) = p.split(',')
        print "%s.%s(%ff,%ff);" % (obj, mode, float(x), float(y))
