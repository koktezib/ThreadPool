# Отчёт по реализации кастомного пула потоков

## 1. Анализ производительности

Кастомный пул потоков был реализован для демонстрации гибкой настройки и расширяемости в условиях высокой нагрузки. В сравнении со стандартным `ThreadPoolExecutor` (Java) и промышленными аналогами (например, пул потоков Tomcat или Jetty):

- **Стандартные решения** оптимизированы для большинства типовых сценариев, используют продвинутые схемы управления очередями, автоматическое масштабирование и work-stealing.
- **Кастомный пул** проще адаптируется под специфические требования (например, особые политики отказа, балансировка, гарантии по `minSpareThreads`), но проигрывает по производительности в экстремальных и сильно конкурентных сценариях.
- В простых случаях (до 10–20 потоков) разница в производительности минимальна. На длинных очередях и при большом количестве потоков стандартные решения проявляют лучшую стабильность и масштабируемость за счёт оптимизаций на уровне JVM.

**Вывод:**  
Свой пул хорош для изучения, гибкой настройки, интеграции специфичных политик (например, собственная балансировка и отказ), но для реальных высоконагруженных систем стоит использовать battle-tested реализации.

---

## 2. Исследование влияния параметров пула на производительность

- **corePoolSize** — увеличивает базовую пропускную способность пула; слишком большое значение приведёт к росту потребления памяти и контекстных переключений.
- **maxPoolSize** — ограничивает максимальное число параллельно работающих потоков. Оптимум зависит от количества процессорных ядер и характера задач (CPU-bound или IO-bound).
- **queueSize** — чем выше, тем больше задач может быть поставлено "в очередь" без немедленного отказа; слишком большая очередь увеличивает время ожидания задачи.
- **minSpareThreads** — обеспечивает минимальный запас свободных потоков для быстрого отклика на всплески нагрузки. Хорошее значение — 10–20% от maxPoolSize.
- **keepAliveTime** — малое значение позволяет быстрее освобождать ресурсы при спаде нагрузки, большое — уменьшает расходы на частое создание/завершение потоков.

Для большинства приложений оптимальным является баланс между числом потоков и длиной очереди. Например, если задач много и они краткоживущие — увеличивать `maxPoolSize`, если длинные — увеличивать `queueSize`.

---

## 3. Механизм распределения задач и балансировка

В реализации использован механизм **round-robin**:

- На каждый поток выделяется собственная очередь.
- Новые задачи добавляются поочерёдно в очереди (или в первую свободную), что обеспечивает равномерную загрузку потоков.
- Если в пуле есть `minSpareThreads`, новые потоки создаются, когда свободных становится меньше указанного порога.
- Такой подход хорошо работает при равномерных нагрузках и прост для отладки.
- В промышленном варианте может применяться алгоритм наименьшей загруженности (*least loaded*) или *work-stealing* (воркер может забирать задачи из чужих очередей).

---

**Заключение:**  
Реализация позволяет гибко настраивать основные параметры, использовать свои политики отказа и логирование, а также демонстрирует все стадии жизненного цикла задач и потоков — от поступления до завершения/отказа.
