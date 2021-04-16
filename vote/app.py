from flask import Flask, render_template, request, make_response, g
from pymongo import MongoClient
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
Get the mongo db connection
:return: mongo db connection
"""
def get_mongo():
    try:
        print('Connection attempt')
        client = MongoClient('mongo', 27017)
        return client
    except pymongo.errors.ServerSelectionTimeoutError as err:
        print(err)
        return -1


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
        client = get_mongo()
        collection = client.vote.votes
        vote = request.form['vote']
        name = request.form['name']
        vote_date = date.today().strftime("%Y%m%d")
        vote_uid = hex(random.getrandbits(64))[2:-1]
        print('Processing vote')
        result=collection.insert_one({'voter_id': voter_id, 'vote': vote, 'name': name, 'date': vote_date, 'vote_uid': vote_uid})
        print('Done {0} as {1}'.format(vote,result.inserted_id))


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
