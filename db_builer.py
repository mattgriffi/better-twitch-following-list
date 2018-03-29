"""This program is for building a sqlite db for my app."""


import requests
import sqlite3
import time
from collections import namedtuple


WAIT_TIME = 60 * 60


Game = namedtuple('Game', ['id', 'name', 'box_art_url'])
User = namedtuple('User', ['id', 'login', 'display_name', 'profile_image_url'])
Stream = namedtuple('Stream', ['id', 'game_id', 'type', 'title', 'viewer_count',
                    'started_at', 'language', 'thumbnail_url'])


headers = {'Client-ID': '6mmva5zc6ubb4j8zswa0fg6dv3y4xw'}
streams_url = 'https://api.twitch.tv/helix/streams'
users_url = 'https://api.twitch.tv/helix/users'
games_url = 'https://api.twitch.tv/helix/games'
top_games_url = 'https://api.twitch.tv/helix/games/top'


def main():
    connection = sqlite3.connect("channels.db")
    create_follows_table(connection)
    create_streams_table(connection)
    create_games_table(connection)
    create_users_table(connection)
    c = connection.cursor()

    success_games = 0
    success_users = 0
    while True:
        # Add games
        print('Starting games update.')
        try:
            games = get_top_games()
            c.executemany('INSERT OR IGNORE INTO games VALUES (?, ?, ?)', games)
            connection.commit()
            success_games += 1
            print(f'Games update complete. Successful games updates: {success_games}')
        except Exception:
            print('Error with games update.')
        wait(60)
        # Add users
        print('Starting users update.')
        try:
            users = get_top_stream_users()
            c.executemany('INSERT OR IGNORE INTO users VALUES (?, ?, ?, ?)', users)
            connection.commit()
            success_users += 1
            print(f'Users update complete. Successful users updates: {success_users}')
        except Exception:
            print('Error with users update.')
        wait(WAIT_TIME)


def wait(seconds):
    for i in range(seconds):
        m, s = divmod(seconds - i, 60)
        print('Sleeping {:2d}:{:02d}...'.format(m, s), end='\r')
        time.sleep(1)


def get_top_stream_users():
    params = {'first': '100', 'after': None}
    users = set()
    for i in range(15):
        print(f'User update {i}...')
        user_ids = set()
        # Make the top streams request
        r = requests.get(streams_url, params=params, headers=headers)
        j = r.json()
        params['after'] = j['pagination']['cursor']
        # Get the user_ids from the streams
        for stream in j['data']:
            user_ids.add(stream['user_id'])
        # Get the users data
        users_params = {'id': list(str(x) for x in user_ids)}
        j2 = requests.get(users_url, params=users_params, headers=headers).json()
        for user in j2['data']:
            users.add(User(user['id'], user['login'],
                           user['display_name'], user['profile_image_url']))
    return users


def get_top_games():
    params = {'first': '100', 'after': None}
    games = set()
    for i in range(20):
        print(f'Game update {i}...')
        r = requests.get(top_games_url, params=params, headers=headers)
        j = r.json()
        try:
            params['after'] = j['pagination']['cursor']
            for game in j['data']:
                games.add(Game(game['id'], game['name'], game['box_art_url']))
        except KeyError:
            break
    return games


def create_streams_table(c):
    s = '''CREATE TABLE IF NOT EXISTS streams (
        _id INTEGER PRIMARY KEY,
        game_id INTEGER NOT NULL DEFAULT 0,
        type INTEGER NOT NULL DEFAULT 0,
        title TEXT NOT NULL DEFAULT "",
        viewer_count INTEGER NOT NULL DEFAULT 0,
        started_at INTEGER NOT NULL DEFAULT 0,
        language TEXT NOT NULL DEFAULT "en",
        thumbnail_url TEXT NOT NULL DEFAULT "")'''
    c.execute(s)


def create_games_table(c):
    s = '''CREATE TABLE IF NOT EXISTS games (
        _id INTEGER PRIMARY KEY,
        name TEXT NOT NULL DEFAULT "",
        box_art_url TEXT NOT NULL DEFAULT "")'''
    c.execute(s)


def create_users_table(c):
    s = '''CREATE TABLE IF NOT EXISTS users (
        _id INTEGER PRImARY KEY,
        login TEXT NOT NULL DEFAULT "",
        display_name TEXT NOT NULL DEFAULT "",
        profile_image_url TEXT NOT NULL DEFAULT
        "https://www-cdn.jtvnw.net/images/xarth/404_user_300x300.png")'''
    c.execute(s)


def create_follows_table(c):
    s = '''CREATE TABLE IF NOT EXISTS follows (
        _id INTEGER PRIMARY KEY,
        pinned INTEGER NOT NULL DEFAULT 0,
        dirty INTEGER NOT NULL DEFAULT 0)'''
    c.execute(s)


if __name__ == "__main__":
    main()
