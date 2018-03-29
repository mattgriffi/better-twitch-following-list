"""This program is for building a sqlite db for my app."""


import sqlite3


def main():
    connection = sqlite3.connect("channels.db")
    c = connection.cursor()

    # Get all the games
    c.execute("SELECT * FROM games")
    with open('games_inserts.txt', 'w', encoding="utf-8") as file:
        base = "INSERT INTO games VALUES {};"
        values = '\n({}, "{}", "{}")'
        values_list = []
        for row in c:
            if (row[0] < 200000000):
                print(row)
                values_list.append(values.format(
                    row[0],
                    row[1].replace('"', r'""'),
                    row[2].replace('"', r'""')
                ))
        file.write(base.format(", ".join(values_list)))

    # Get all the users
    c.execute("SELECT * FROM users")
    with open('users_inserts.txt', 'w', encoding="utf-8") as file:
        base = "INSERT INTO users VALUES {};"
        values = '\n({}, "{}", "{}", "{}")'
        values_list = []
        for row in c:
            if (row[0] < 200000000):
                print(row)
                values_list.append(values.format(
                    row[0],
                    row[1].replace('"', r'""'),
                    row[2].replace('"', r'""'),
                    row[3].replace('"', r'""')
                ))
        file.write(base.format(", ".join(values_list)))


if __name__ == "__main__":
    main()
