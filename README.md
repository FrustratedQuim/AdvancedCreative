# С чем это есть?
## Зависимости

На сервере должна быть парочка библиотек.
- PacketEvents 2.9.4 - https://github.com/retrooper/packetevents
- EntityLib 2.4.11 - https://github.com/Tofaa2/EntityLib

## Распределение прав

Раскидать по нужным привилегиям.

- Привилегия - Пермишен
  - **Player**
    - `advancedcreative.ahelp`
      - Открытие хелп-сводки плагина.
    - `advancedcreative.sit`
      - Возможность сидеть командой + пкм по полублоку/ступенькам.
    - `advancedcreative.lay`
      - Возможность лежать командой + пкм по кровати.
    - `advancedcreative.crawl`
      - Возможность включить режим ползания.
    - `advancedcreative.hide`
      - Возможность визуально скрывать игроков, включая выкидываемые ими предмета и проджектайлы.
    - `advancedcreative.strength`
      - Возможность изменять свой атрибут силы.
    - `advancedcreative.health`
      - Возможность изменять свой атрибут здоровья.
    - `advancedcreative.effects`
      - Возможность переключать пермаментные эффекты от зелий.
    - `advancedcreative.itemdb`
      - Получить ID предмета
    - `advancedcreative.id`
      - Короткий аналог /itemdb
  - **Rise**
    - `advancedcreative.sneeze`
      - Возможность визуально чихнуть командой.
    - `advancedcreative.glide`
      - Возможность включить парение на элитрах, без элитр.
    - `advancedcreative.gravity`
      - Возможность изменять свой атрибут гравитации.
  - **Flare**
    - `advancedcreative.resize`
      - Возможность изменять свой атрибут размера.
    - `advancedcreative.freeze`
      - Возможность визуально заморозить себя командой.
    - `advancedcreative.bind`
      - Возможность забиндить исполнение команды на предмет/руку.
  - **Spark**
    - `advancedcreative.glow`
      - Возможность переключать своё свечение.
    - `advancedcreative.disguise`
      - Возможность превратиться в моба.
  - **Sunny**
    - `advancedcreative.sit.head`
      - Возможность сесть на голову игрока при помощи пкм.
  - **Horizon**
    - `advancedcreative.spit`
      - Возможность визуально плюнуть командой.
    - `advancedcreative.piss`
      - Возможность пись-пись-пись командой
    - `advancedcreative.disguise.full`
      - Открытие возможности превращаться в Визера/Вардена/Эндер-дракона/Гиганта.
  - **Moder**
    - `advancedcreative.slap`
      - Возможность переключить режим пощёчины: откидывание игрока при ударе.
    - `advancedcreative.sithead`
      - Возможность садить игроков на головы командой.
    - `advancedcreative.hide.bypass`
      - Байпас на попытку скрыть себя командой скрытия.
    - `advancedcreative.freeze.other`
      - Возможность замораживать других игроков командой.
    - `advancedcreative.effects.admin`
      - Возможность переключать пермаментные эффекты от зелий другим.
  - **Admin**
    - `advancedcreative.slap.bypass`
      - Байпас на попытку дать себе пощёчину.

- Условные обозначения:
  - В группу *Moder* входит весь персонал — Moder(1,2,3), AntiCheat, их лидеры.
  - В группу *Admin* входит высшая администрация.
  - Вышестоящая привилегия имеет все права нижестоящей соответственно.
