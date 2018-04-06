import requests
import json


headers = {'Client-ID': '6mmva5zc6ubb4j8zswa0fg6dv3y4xw'}
streams_url = 'https://api.twitch.tv/helix/streams'
users_url = 'https://api.twitch.tv/helix/users'
games_url = 'https://api.twitch.tv/helix/games'
top_games_url = 'https://api.twitch.tv/helix/games/top'
old_api = 'https://api.twitch.tv/kraken/channels/104962503?api_version=5'
ids = [30084132, 29183589, 22776112, 21685437]
ids2 = [28577834]
154723532,67650991,24147592,110690086,37138771


def main():
    pprint_json(test_url('https://api.twitch.tv/kraken/streams/?channel=154723532,67650991,24147592,110690086,37138771&api_version=5'))


def main5():
    pprint_json(get_user('TSM_Myth'))
    pprint_json(get_user('Gotaga'))
    pprint_json(get_user('RealKraftyy'))
    pprint_json(get_user('apored'))
    pprint_json(get_user('uclaoboat'))


def main2():
    pprint_json(test_url('https://api.twitch.tv/helix/users/follows?first=100&from_id=66712091&after=eyJiIjpudWxsLCJhIjoiIn0'))


def main4():
    pprint_json(test_url('https://api.twitch.tv/helix/streams?user_id=8822&user_id=12826&user_id=2158531&user_id=2783476&user_id=8330235&user_id=9235733&user_id=9679595&user_id=10180554&user_id=10663608&user_id=10794219&user_id=10817445&user_id=12616386&user_id=13784804&user_id=13821412&user_id=14410317&user_id=14836307&user_id=17337557&user_id=18587270&user_id=18730955&user_id=19092542&user_id=19270428&user_id=19397907&user_id=20248706&user_id=20526480&user_id=20687650&user_id=20694610&user_id=20992865&user_id=21685437&user_id=21727348&user_id=21733581&user_id=21841789&user_id=22043840&user_id=22320080&user_id=22344826&user_id=22365608&user_id=22449618&user_id=22776112&user_id=22859340&user_id=22900833&user_id=23161357&user_id=23202645&user_id=23278374&user_id=23473417&user_id=23822990&user_id=23969535&user_id=23973651&user_id=24522509&user_id=24687283&user_id=24811779&user_id=25220444&user_id=25681189&user_id=25766032&user_id=26301881&user_id=26490481&user_id=26535320&user_id=26610234&user_id=26973975&user_id=27396889&user_id=27446517&user_id=27631292&user_id=28412126&user_id=28577834&user_id=28761771&user_id=29183589&user_id=29325859&user_id=29478753&user_id=29714457&user_id=29795919&user_id=29970326&user_id=30080810&user_id=30084132&user_id=30220059&user_id=30284904&user_id=30672745&user_id=30934300&user_id=31259250&user_id=31723226&user_id=32179353&user_id=35049749&user_id=35077780&user_id=35834033&user_id=36029255&user_id=36440656&user_id=36464665&user_id=36551454&user_id=36619478&user_id=36769016&user_id=36990968&user_id=37116492&user_id=37121843&user_id=37136273&user_id=37141180&user_id=37599722&user_id=38270518&user_id=38290568&user_id=38451834&user_id=38467083&user_id=38640167&user_id=38718052&user_id=38961964'))


def main3():
    pprint_json(get_user('PrincezzxDiana'))
    pprint_json(get_stream(22900833))
    # s = 
    # pprint_json(test_url(s))
    # print(s.count('user_id'))


def test_url(url):
    r = requests.get(url, headers=headers)
    return r.json()


def get_user(user):
    if isinstance(user, str):
        params = {'login': user}
    else:
        params = {'id': user}
    r = requests.get(users_url, params=params, headers=headers)
    return r.json()


def get_stream(user_id):
    params = {'user_id': user_id, 'type': 'all'}
    r = requests.get(streams_url, params=params, headers=headers)
    return r.json()


def pprint_json(json_dict):
    print(json.dumps(json_dict, indent=2))


if __name__ == "__main__":
    main()
