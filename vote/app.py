from flask import Flask, render_template, request, make_response, g
from redis import Redis
from datetime import date
import os
import socket
import random
import json

option_a = os.getenv('OPTION_A', "Fleur")
option_b = os.getenv('OPTION_B', "Valise")
hostname = socket.gethostname()

app = Flask(__name__)

"""
Get the redis connection
:return: redis connection
"""
def get_redis():
    if not hasattr(g, 'redis'):
        g.redis = Redis(host="redis", db=0, socket_timeout=5)
    return g.redis

"""
Method to show and submit the form, responding to either get or post call.
:return: Response http
"""
@app.route("/", methods=['POST','GET'])
def hello():
    voter_id = request.cookies.get('voter_id')
    if not voter_id:
        voter_id = hex(random.getrandbits(64))[2:-1]

    vote = None
    name = ""

    if request.method == 'POST':
        redis = get_redis()
        vote = request.form['vote']
        name = request.form['name']
        vote_date = date.today().strftime("%Y%m%d")
        data = json.dumps({'voter_id': voter_id, 'vote': vote, 'name': name, 'date': vote_date})
        redis.rpush('votes', data)

    resp = make_response(render_template(
        'index.html',
        option_a=option_a,
        option_b=option_b,
        hostname=hostname,
        name=name,
        vote=vote,
    ))
    resp.set_cookie('voter_id', voter_id)
    return resp


if __name__ == "__main__":
    app.run(host='0.0.0.0', port=80, debug=True, threaded=True)
