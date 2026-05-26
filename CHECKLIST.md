# Backend — Checklist de Implementações

## Segurança & Autorização

- [x] **Analytics por dono do post ou super-user**
  - Endpoint `GET /api/events/summary` atualmente é público (`permitAll`).
  - Criar role `ROLE_SUPERUSER` no sistema de usuários.
  - Nos endpoints `GET /api/events/**`, verificar se o usuário autenticado é dono do `postId` solicitado ou possui `ROLE_SUPERUSER`. Anonimos → 403.
  - Ajustar `SecurityConfig`: remover o `permitAll` do `GET /api/events/**` e tratar a autorização dentro do `EventService` ou via `@PreAuthorize`.

## Segurança — Continuação

- [ ] **Rate limiting no `POST /api/events/register`**
  - Endpoint público sem proteção contra spam — eventos falsos inflam as estatísticas.
  - Implementar com Bucket4j (ou similar): limitar por IP, ex: 30 requisições/minuto.
  - Retornar `429 Too Many Requests` quando o limite for atingido.

- [ ] **Refresh token**
  - JWT atual expira em 24h sem possibilidade de renovação — usuário precisa fazer login novamente.
  - Criar entidade `RefreshToken` (token opaco, TTL longo, armazenado no banco).
  - Adicionar endpoint `POST /api/auth/refresh` que valida o refresh token e devolve novo JWT.
  - Adicionar endpoint `POST /api/auth/logout` que invalida o refresh token.

## Integridade de Dados

- [ ] **Limpar links de posts ao excluir um post referenciado**
  - Quando um post é deletado, outros posts podem ter o `id` dele na lista de links — esses links ficam órfãos/inválidos.
  - No `PostService`, após o delete, buscar todos os posts que contêm o `id` deletado em sua lista de links e removê-lo.
  - Alternativa via banco: adicionar `ON DELETE` trigger ou listener JPA (`@PreRemove`) que dispara a limpeza automaticamente.
  - Garantir que o frontend não quebre ao receber um link cujo post não existe mais (retornar 404 limpo).
  - Ao deletar um post, deletar também todos os `Event` associados ao seu `postId` (ou via `@PreRemove` / `ON DELETE CASCADE` na FK).

## Qualidade da API

- [ ] **Paginação nos endpoints de listagem**
  - `GET /api/posts/verPosts` e `GET /api/events/byPost` retornam todos os registros de uma vez.
  - Adicionar suporte a `?page=0&size=20` usando `Pageable` do Spring Data.
  - Retornar `Page<T>` com metadados de totalElements, totalPages, etc.

- [ ] **Validação de entrada com `@Valid`**
  - DTOs de request não têm anotações de validação — requests malformadas chegam até o service.
  - Anotar campos obrigatórios com `@NotNull`, `@NotBlank`, `@Size`, etc.
  - Adicionar `@Valid` nos parâmetros dos controllers e um `@ControllerAdvice` para retornar `400` com mensagem legível.

## Operacional

- [ ] **CORS via variável de ambiente**
  - Origem hardcoded em `localhost:3000` quebra em qualquer deploy fora do ambiente local.
  - Externalizar para `application.properties` como `cors.allowed-origins` e injetar via `@Value`.

- [ ] **Cache no `GET /api/events/summary`**
  - A query executa três agregações full-scan na tabela `events` a cada chamada.
  - Adicionar `@Cacheable` com Spring Cache (Caffeine ou Redis).
  - Invalidar o cache ao registrar um novo evento via `@CacheEvict` no `registerEvent`.

## Mídia & Upload

- [ ] **Upload de imagens nos posts**
  - Adicionar endpoint `POST /api/posts/{id}/image` que recebe `multipart/form-data`.
  - Definir estratégia de armazenamento: disco local (`/uploads`) ou object storage (S3/MinIO).
  - Salvar path/URL da imagem no Post entity (novo campo `imageUrl`).
  - Adicionar validação de tipo (`image/jpeg`, `image/png`, `image/webp`) e tamanho máximo.
  - Servir os arquivos via endpoint público `GET /api/posts/images/{filename}` ou via CDN.
  - Ao deletar um post, garantir que a imagem associada também seja removida.
