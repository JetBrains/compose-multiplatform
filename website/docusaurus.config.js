

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

const config = {
  title: 'Compose Multiplatform',
  tagline: 'Compose Multiplatform is a declarative framework for sharing UIs across multiple platforms with Kotlin',
  favicon: 'img/favicon.ico',
  url: 'https://your-docusaurus-test-site.com',
  baseUrl: '/',
  organizationName: 'jetbrains', // Usually your GitHub org/user name.
  projectName: 'compose-multiplatform', // Usually your repo name.
  onBrokenLinks: 'throw',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      ({
        docs: {
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl:'https://github.com/jetbrains/compose-multiplatform/tree/master/website',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
        blog: false
      }),
    ],
  ],

  themeConfig:
    ({
      image: 'img/docusaurus-social-card.jpg',
      announcementBar: {
        id: 'announcementBar',
        content: `📲  <a href="https://github.com/JetBrains/compose-multiplatform-ios-android-template/">Effortlessly share UIs between iOS and Android. Try Compose Multiplatform now!</a>`
      },
      navbar: {
        title: 'Compose Multiplatform',
        logo: {
          alt: 'Website Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            href: 'https://github.com/jetbrains/compose-multiplatform',
            position: 'right',
            className: 'header-github-link',
            'aria-label': 'GitHub repository'
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Tutorial',
                to: '/docs/intro',
              },
            ],
          },
          // {
          //   title: 'Community',
          //   items: [
          //     {
          //       label: 'Stack Overflow',
          //       href: 'https://stackoverflow.com/questions/tagged/docusaurus',
          //     },
          //     {
          //       label: 'Discord',
          //       href: 'https://discordapp.com/invite/docusaurus',
          //     },
          //     {
          //       label: 'Twitter',
          //       href: 'https://twitter.com/docusaurus',
          //     },
          //   ],
          // },
          {
            title: 'More',
            items: [
              // {
              //   label: 'Blog',
              //   to: '/blog',
              // },
              {
                label: 'GitHub',
                href: 'https://github.com/jetbrains/compose-multiplatform',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} The Jetbrains Community. Built with Docusaurus.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: ['kotlin', 'groovy'],
      },
    }),
};

module.exports = config;
