# Как внести вклад

Спасибо за интерес к Eidos. Ниже — минимум, который нужно знать, чтобы ваш
вклад приняли без лишних кругов.

## Лицензия вклада

Проект распространяется под [Apache License 2.0](LICENSE). Отправляя изменения,
вы соглашаетесь, что они будут выпущены под этой же лицензией.

## Подпись коммитов (DCO)

Мы используем **Developer Certificate of Origin** вместо соглашения о передаче
прав. Это проще: отдельный документ подписывать не нужно, достаточно строки в
каждом коммите.

Подпись ставится флагом `-s`:

```
git commit -s -m "Краткое описание изменения"
```

Он добавит в сообщение строку вида:

```
Signed-off-by: Имя Фамилия <email@example.com>
```

Имя и адрес должны быть настоящими — псевдонимы и анонимные адреса не
принимаются. Забыли подписать последний коммит — поправьте его через
`git commit --amend -s`; несколько коммитов — `git rebase --signoff`.

Ставя подпись, вы подтверждаете текст ниже.

## Процесс

1. Обсудите крупное изменение в issue до того, как писать код: так проще
   разойтись во мнениях на словах, а не на готовом патче.
2. Ветку делайте от `main`.
3. Один pull request — одно изменение. Несвязанные правки разносите по разным.
4. Тесты обязательны для нового поведения и для исправленных ошибок. Прогон:
   `./mvnw test` в java-сервисах, `npm test` в консолях.
5. Сообщение коммита: первая строка — что изменилось, дальше — почему. «Почему»
   важнее «что»: что сделано, видно из диффа.

## Стиль

Пишите код так, как написан окружающий: те же идиомы, та же плотность
комментариев, то же именование. Комментарии в проекте на русском —
придерживайтесь этого в изменяемых файлах.

Каждый новый файл с исходным кодом начинается с лицензионного заголовка Apache;
посмотрите любой существующий файл как образец.

## Сообщения об уязвимостях

Не через issue и не через pull request — см. [SECURITY.md](SECURITY.md).

---

```
Developer Certificate of Origin
Version 1.1

Copyright (C) 2004, 2006 The Linux Foundation and its contributors.

Everyone is permitted to copy and distribute verbatim copies of this
license document, but changing it is not allowed.


Developer's Certificate of Origin 1.1

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
    have the right to submit it under the open source license
    indicated in the file; or

(b) The contribution is based upon previous work that, to the best
    of my knowledge, is covered under an appropriate open source
    license and I have the right under that license to submit that
    work with modifications, whether created in whole or in part
    by me, under the same open source license (unless I am
    permitted to submit under a different license), as indicated
    in the file; or

(c) The contribution was provided directly to me by some other
    person who certified (a), (b) or (c) and I have not modified
    it.

(d) I understand and agree that this project and the contribution
    are public and that a record of the contribution (including all
    personal information I submit with it, including my sign-off) is
    maintained indefinitely and may be redistributed consistent with
    this project or the open source license(s) involved.
```
