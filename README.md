# С чем это есть?
## Зависимости

На сервере должна быть парочка библиотек.
- PacketEvents 2.9.4 - https://github.com/retrooper/packetevents
- EntityLib 2.4.11 - https://github.com/Tofaa2/EntityLib

## Распределение прав

- Привилегия - Пермишен
  - **Player**
    - `acreative.ahelp`
      - Открытие хелп-сводки плагина.
    - `acreative.sit`
      - Возможность сидеть командой + ПКМ по полублоку/ступенькам.
    - `acreative.lay`
      - Возможность лежать командой + ПКМ по кровати.
    - `acreative.crawl`
      - Возможность включить режим ползания.
    - `acreative.hide`
      - Возможность визуально скрывать игроков, включая выкидываемые ими предметы и проджектайлы.
    - `acreative.strength`
      - Возможность изменять свой атрибут силы.
    - `acreative.health`
      - Возможность изменять свой атрибут здоровья.
    - `acreative.effects`
      - Возможность переключать перманентные эффекты от зелий.
    - `acreative.itemdb`
      - Получить ID предмета.
  - **Rise**
    - `acreative.sneeze`
      - Возможность визуально чихнуть командой.
    - `acreative.glide`
      - Возможность включить парение на элитрах без элитр.
    - `acreative.gravity`
      - Возможность изменять свой атрибут гравитации.
  - **Flare**
    - `acreative.resize`
      - Возможность изменять свой атрибут размера.
    - `acreative.freeze`
      - Возможность визуально заморозить себя командой.
  - **Spark**
    - `acreative.glow`
      - Возможность переключать своё свечение.
    - `acreative.disguise`
      - Возможность превратиться в моба.
  - **Sunny**
    - `acreative.sithead`
      - Возможность сесть на голову игрока при помощи ПКМ и команда на отключение.
  - **Horizon**
    - `acreative.spit`
      - Возможность визуально плюнуть командой.
    - `acreative.piss`
      - Возможность пись-пись-пись командой.
    - `acreative.disguise.extended`
      - Открытие возможности превращаться в Визера, Вардена, Эндер-дракона и Гиганта.
    - `acreative.disguise.nick`
      - Открытие возможности превращаться в мобов, скрывая свой ник.
  - **Moder**
    - `acreative.slap`
      - Возможность переключить режим пощёчины: откидывание игрока при ударе.
    - `acreative.sithead.other`
      - Возможность сажать игроков на головы командой.
    - `acreative.hide.bypass`
      - Байпас на попытку скрыть себя командой скрытия.
    - `acreative.freeze.other`
      - Возможность замораживать других игроков командой.
    - `acreative.effects.other`
      - Возможность переключать перманентные эффекты от зелий другим.
  - **Admin**
    - `acreative.slap.bypass`
      - Байпас на попытку дать себе пощёчину.

- Условные обозначения:
  - В группу **Moder** входит весь персонал: Moder(1,2,3)
  - В группу **Admin** входят Admin
  - Вышестоящая привилегия имеет все права нижестоящей соответственно.

## Separate Extension

- The plugin uses the Minecraft-Heads API to populate the decorative heads menu.

<a href="https://minecraft-heads.com/">
<img width="468" height="60" alt="minecraft-heads_fullbanner_468x60" src="https://github.com/user-attachments/assets/19ff7750-8b9e-4256-9a5a-74329ae4e26b" />
</a>
